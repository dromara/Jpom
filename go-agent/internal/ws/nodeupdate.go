/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package ws

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"

	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/httpx"
)

// defaultUpgradeMessage is Const.UPGRADE_MSG fallback (i18n upgrade_duration_message).
const defaultUpgradeMessage = ""

// binaryMessageCap 是 /node_update 二进制帧的硬上限。Java
// AgentWebSocketUpdateHandle#onOpen 取 min(spring.servlet.multipart.max-request-size, 10MB)。
const binaryMessageCap = 10 << 20

// UpgradeHandler installs an uploaded agent package and restarts the process.
// The binary self-upgrade strategy belongs to the platform/upgrade domain and is
// registered by the api wiring (nil means "not implemented yet").
type UpgradeHandler interface {
	// Restart installs uploadedFile and restarts the agent; it returns the
	// message sent back to the server (the Java agent answers UPGRADE_MSG).
	Restart(uploadedFile string) (string, error)
}

// uploadState is one /node_update upload context
// (AgentWebSocketUpdateHandle.UploadFileModel).
type uploadState struct {
	mu sync.Mutex

	ID           string
	Name         string
	SavePath     string
	Version      string
	TimeStamp    string
	Size         int64
	CompleteSize int64

	file *os.File
}

// Path returns the uploaded file path.
func (u *uploadState) Path() string { return filepath.Join(u.SavePath, u.Name) }

// Append appends one binary slice.
func (u *uploadState) Append(data []byte) error {
	u.mu.Lock()
	defer u.mu.Unlock()
	if u.file == nil {
		file, err := os.OpenFile(u.Path(), os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
		if err != nil {
			return err
		}
		u.file = file
	}
	written, err := u.file.Write(data)
	u.CompleteSize += int64(written)
	return err
}

// Close closes the uploaded file.
func (u *uploadState) Close() {
	u.mu.Lock()
	defer u.mu.Unlock()
	if u.file != nil {
		_ = u.file.Close()
		u.file = nil
	}
}

// Progress returns the updateNode progress payload. Live Java 2.11.13 capture
// (.tmp/audit/ws_java.txt) shows size/completeSize are JSON **strings**
// (fastjson2 long -> string on this endpoint), null fields omitted;
// UploadFileModel does not set timeStamp.
//
// UploadFileModel#getFilePath is a getter, so fastjson2 also serializes it as the
// "filePath" property (savePath + "/" + name).
func (u *uploadState) Progress() map[string]any {
	u.mu.Lock()
	defer u.mu.Unlock()
	data := map[string]any{
		"id":           u.ID,
		"name":         u.Name,
		"size":         strconv.FormatInt(u.Size, 10),
		"completeSize": strconv.FormatInt(u.CompleteSize, 10),
		"savePath":     u.SavePath,
		"filePath":     u.SavePath + "/" + u.Name,
	}
	if u.Version != "" {
		data["version"] = u.Version
	}
	return data
}

// HandleNodeUpdate serves /node_update (AgentWebSocketUpdateHandle).
func (m *Manager) HandleNodeUpdate(c *httpx.Context) {
	session, err := upgrade(c)
	if err != nil {
		return
	}
	var upload *uploadState
	defer func() {
		if upload != nil {
			upload.Close()
		}
		session.Close()
	}()
	if !m.authorized(session, c.R) {
		return
	}
	// 读取上限（P2-7）：文本 5MB；二进制 min(max-request-size, 10MB)。
	session.SetReadLimit(m.binaryReadLimit())
	for {
		messageType, data, err := session.Read()
		if err != nil {
			return
		}
		if messageType == websocket.TextMessage && int64(len(data)) > defaultTextLimit {
			// 文本帧超过 Java @OnMessage 的 5MB 上限，按 Tomcat 语义关闭。
			session.CloseWithReason(closeTooBig, "")
			return
		}
		switch messageType {
		case websocket.TextMessage:
			model, err := decodeObject(data)
			if err != nil {
				continue
			}
			switch bodyString(model, "command") {
			case "getVersion":
				if encoded, err := json.Marshal(m.manifestForWebSocket()); err == nil {
					// 注意：data 是 manifest JSON 的“字符串”，不是对象
					model["data"] = string(encoded)
				}
			case "upload":
				params, _ := model["params"].(map[string]any)
				state, err := m.prepareUpload(bodyString(model, "nodeId"), params)
				if err != nil {
					m.App.Log().Error("prepare upgrade upload failed", "error", err)
					model["data"] = err.Error()
				} else {
					if upload != nil {
						upload.Close()
					}
					upload = state
				}
			case "restart":
				model["data"] = m.restartUpload(session, upload)
			case "heart":
				// 回帧保持连接
			default:
				m.App.Log().Warn("ignored node_update command", "command", bodyString(model, "command"))
			}
			_ = session.SendJSON(model)
		case websocket.BinaryMessage:
			if upload == nil {
				m.App.Log().Warn("binary frame without upload context")
				continue
			}
			if err := upload.Append(data); err != nil {
				m.App.Log().Error("append upgrade package slice failed", "error", err)
				continue
			}
			frame := map[string]any{
				"command": "updateNode",
				"nodeId":  upload.ID,
				"data":    upload.Progress(),
			}
			_ = session.SendJSON(frame)
		}
	}
}

// manifestForWebSocket returns the JpomManifest payload using fastjson2 number
// semantics (pid/upTime are numbers here; HTTP /info uses Jackson strings).
func (m *Manager) manifestForWebSocket() map[string]any {
	manifest := m.App.Manifest()
	payload := make(map[string]any, len(manifest)+1)
	for key, value := range manifest {
		payload[key] = value
	}
	if pid, err := strconv.ParseInt(fmt.Sprint(manifest["pid"]), 10, 64); err == nil {
		payload["pid"] = pid
	}
	if upTime, err := strconv.ParseInt(fmt.Sprint(manifest["upTime"]), 10, 64); err == nil {
		payload["upTime"] = upTime
	}
	return payload
}

// prepareUpload creates the upload context: <data>/temp/<yyyy-MM-dd>/<name> after
// removing a previous file with the same name (AgentConfig#getTempPath).
func (m *Manager) prepareUpload(nodeID string, params map[string]any) (*uploadState, error) {
	name := bodyString(params, "name")
	if name == "" || filepath.Base(name) != name {
		return nil, fmt.Errorf("invalid upgrade package name: %s", name)
	}
	// AgentConfig.getTempPath() = JpomApplication.getTempPath() + yyyy-MM-dd
	savePath, err := filepath.Abs(m.App.TempPath())
	if err != nil {
		return nil, err
	}
	if err := os.MkdirAll(savePath, 0o755); err != nil {
		return nil, err
	}
	state := &uploadState{
		ID:        nodeID,
		Name:      name,
		SavePath:  savePath,
		Version:   bodyString(params, "version"),
		TimeStamp: bodyString(params, "timeStamp"),
		Size:      int64(bodyInt(params, "size")),
	}
	_ = os.Remove(state.Path())
	return state, nil
}

// binaryReadLimit 返回 /node_update 二进制帧上限：
// min(spring.servlet.multipart.max-request-size, 10MB)（Java onOpen 同语义）。
func (m *Manager) binaryReadLimit() int64 {
	limit := int64(0)
	if m.App != nil && m.App.Config != nil {
		limit = m.App.Config.MaxRequestSize()
	}
	if limit <= 0 || limit > binaryMessageCap {
		limit = binaryMessageCap
	}
	return limit
}

// restartUpload installs the uploaded package and reports the result message.
// 响应 data 必须按会话语言本地化（不能把 i18n key 原样回给前端，P2-8）。
func (m *Manager) restartUpload(session *Session, upload *uploadState) string {
	if upload == nil {
		return m.translate(session, "i18n.restart_failed.f92a") + "no uploaded package"
	}
	upload.Close()
	if m.Upgrade == nil {
		return m.translate(session, "i18n.restart_failed.f92a") + "go agent self upgrade is not implemented"
	}
	message, err := m.Upgrade.Restart(upload.Path())
	if err != nil {
		return m.translate(session, "i18n.restart_failed.f92a") + err.Error()
	}
	if message == "" {
		return m.translate(session, "i18n.upgrade_duration_message.bab4")
	}
	return m.localizeMessage(session, message)
}

// localizeMessage 把 sysinfo 返回的 i18n key 按会话语言渲染（非 key 原样返回）。
func (m *Manager) localizeMessage(session *Session, message string) string {
	if strings.HasPrefix(message, "i18n.") {
		return m.translate(session, message)
	}
	return message
}
