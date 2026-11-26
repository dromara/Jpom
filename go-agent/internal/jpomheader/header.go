//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

// Package jpomheader holds the Jpom protocol header/query names shared by the
// agent HTTP endpoints, the WebSocket handshake and the reverse tunnel.
//
// The literals live only here: Java keeps the same names in TransportServer, and
// preflight-check.sh --ua fails when a second literal shows up on either side.
package jpomheader

const (
	// UserAgent is the outbound identity header (mirrors Java TransportServer.USER_AGENT).
	UserAgent = "User-Agent"
	// Authorize is the agent authorization digest (header and WS query parameter).
	Authorize = "Jpom-Agent-Authorize"
	// WorkspaceID carries the workspace of the caller (header and WS query parameter).
	WorkspaceID = "workspaceId"
	// ServerUserName carries the server side login name forwarded to the agent.
	ServerUserName = "Jpom-Server-UserName"
	// TransportEncryption marks the request body encryption mode.
	TransportEncryption = "transport-encryption"
	// AcceptLanguage selects the response language.
	AcceptLanguage = "Accept-Language"
	// ContentType is the request/response media type.
	ContentType = "Content-Type"
)
