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
	"sync"

	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/httpx"
)

// upgradeDurationMessage is Const.UPGRADE_MSG.
const upgradeDurationMessage = "升级(重启)中大约需要30秒～2分钟左右"

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

// Progress returns the updateNode progress payload (fastjson2 semantics: long
// numbers, null fields omitted; UploadFileModel does not set timeStamp).
//
// UploadFileModel#getFilePath is a getter, so fastjson2 also serializes it as the
// "filePath" property (savePath + "/" + name).
func (u *uploadState) Progress() map[string]any {
	u.mu.Lock()
	defer u.mu.Unlock()
	data := map[string]any{
		"id":           u.ID,
		"name":         u.Name,
		"size":         u.Size,
		"completeSize": u.CompleteSize,
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
	session.SetReadLimit(defaultTextLimit)
	for {
		messageType, data, err := session.Read()
		if err != nil {
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
				model["data"] = m.restartUpload(upload)
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
		return nil, fmt.Errorf("升级包文件名不正确: %s", name)
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

// restartUpload installs the uploaded package and reports the result message.
func (m *Manager) restartUpload(upload *uploadState) string {
	if upload == nil {
		return "重启失败：没有上传的升级包"
	}
	upload.Close()
	if m.Upgrade == nil {
		return "重启失败：Go agent 自升级尚未实现（等待平台升级域提供 UpgradeHandler）"
	}
	message, err := m.Upgrade.Restart(upload.Path())
	if err != nil {
		return "重启失败：" + err.Error()
	}
	if message == "" {
		return upgradeDurationMessage
	}
	return message
}
