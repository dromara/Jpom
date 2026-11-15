/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package script

import (
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sort"
)

// Record is one entry of the agent JSON data files (script.json et al).
//
// The Java agent keeps these files as a JSON object keyed by the record id and
// relies on the insertion order of the underlying LinkedHashMap: list.json
// returns the records in file order. Go maps are unordered, so the file is read
// and written as an ordered list of records.
type Record struct {
	Key   string
	Value map[string]any
}

// scriptFieldOrder is the serialization order used by the Java NodeScriptModel
// (field order of the class hierarchy). Keeping it makes the files readable for
// the Java agent and eases golden response comparison.
var scriptFieldOrder = []string{
	"id", "name", "workspaceId", "nodeId", "modifyUser", "createTime", "modifyTime", "createUser",
	"lastRunUser", "context", "autoExecCron", "defArgs", "description", "scriptType",
}

// execLogFieldOrder mirrors NodeScriptExecLogModel.
var execLogFieldOrder = []string{
	"id", "name", "workspaceId", "nodeId", "modifyUser", "createTime", "modifyTime", "createUser",
	"createTimeMillis", "scriptId", "scriptName", "triggerExecType",
}

// libraryFieldOrder mirrors ScriptLibraryModel.
var libraryFieldOrder = []string{"id", "tag", "script", "description", "version"}

// readRecords reads a JSON object file preserving the top level key order.
// A missing or empty file yields an empty slice.
func readRecords(path string) ([]Record, error) {
	content, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
	}
	trimmed := bytes.TrimSpace(content)
	if len(trimmed) == 0 {
		return nil, nil
	}
	decoder := json.NewDecoder(bytes.NewReader(trimmed))
	decoder.UseNumber()
	token, err := decoder.Token()
	if err != nil {
		return nil, fmt.Errorf("parse %s failed: %w", filepath.Base(path), err)
	}
	if delim, ok := token.(json.Delim); !ok || delim != '{' {
		return nil, fmt.Errorf("%s is not a valid JSON object", filepath.Base(path))
	}
	records := make([]Record, 0, 16)
	for decoder.More() {
		keyToken, err := decoder.Token()
		if err != nil {
			return nil, fmt.Errorf("parse %s failed: %w", filepath.Base(path), err)
		}
		key, ok := keyToken.(string)
		if !ok {
			return nil, fmt.Errorf("%s contains an invalid key", filepath.Base(path))
		}
		var raw json.RawMessage
		if err := decoder.Decode(&raw); err != nil {
			return nil, fmt.Errorf("parse %s entry %s failed: %w", filepath.Base(path), key, err)
		}
		value := map[string]any{}
		if err := decodeJSON(raw, &value); err != nil {
			return nil, fmt.Errorf("parse %s entry %s failed: %w", filepath.Base(path), key, err)
		}
		records = append(records, Record{Key: key, Value: value})
	}
	// consume the closing brace
	_, _ = decoder.Token()
	return records, nil
}

// decodeJSON decodes raw JSON keeping numbers as json.Number so that ids,
// millisecond timestamps and ports are re-written verbatim.
func decodeJSON(raw []byte, dst any) error {
	decoder := json.NewDecoder(bytes.NewReader(raw))
	decoder.UseNumber()
	return decoder.Decode(dst)
}

// readObject reads a single JSON object file (library entry, env var file...).
func readObject(path string, dst any) error {
	content, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}
	trimmed := bytes.TrimSpace(content)
	if len(trimmed) == 0 {
		return nil
	}
	return decodeJSON(trimmed, dst)
}

// writeRecords atomically replaces the JSON object file keeping record order and
// the field order declared by order.
func writeRecords(path string, records []Record, order []string) error {
	var buffer bytes.Buffer
	buffer.WriteByte('{')
	for index, record := range records {
		if index > 0 {
			buffer.WriteByte(',')
		}
		key, err := json.Marshal(record.Key)
		if err != nil {
			return err
		}
		buffer.Write(key)
		buffer.WriteByte(':')
		value, err := marshalOrdered(record.Value, order)
		if err != nil {
			return err
		}
		buffer.Write(value)
	}
	buffer.WriteByte('}')
	return atomicWrite(path, buffer.Bytes())
}

// marshalOrdered serializes an object with the given field order first, then the
// remaining fields sorted alphabetically (any historical/unknown field is kept).
func marshalOrdered(value map[string]any, order []string) ([]byte, error) {
	if value == nil {
		return []byte("null"), nil
	}
	written := make(map[string]struct{}, len(value))
	keys := make([]string, 0, len(value))
	for _, key := range order {
		if _, ok := value[key]; ok {
			keys = append(keys, key)
			written[key] = struct{}{}
		}
	}
	rest := make([]string, 0, len(value))
	for key := range value {
		if _, ok := written[key]; ok {
			continue
		}
		rest = append(rest, key)
	}
	sort.Strings(rest)
	keys = append(keys, rest...)
	var buffer bytes.Buffer
	buffer.WriteByte('{')
	for index, key := range keys {
		if index > 0 {
			buffer.WriteByte(',')
		}
		encodedKey, err := json.Marshal(key)
		if err != nil {
			return nil, err
		}
		buffer.Write(encodedKey)
		buffer.WriteByte(':')
		encodedValue, err := json.Marshal(value[key])
		if err != nil {
			return nil, err
		}
		buffer.Write(encodedValue)
	}
	buffer.WriteByte('}')
	return buffer.Bytes(), nil
}

// atomicWrite writes data to a temporary file in the same directory and renames
// it on top of path, so a crash never leaves a half written data file behind
// (the Java agent may read the same directory after a switch).
func atomicWrite(path string, data []byte) error {
	dir := filepath.Dir(path)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	tmp, err := os.CreateTemp(dir, ".jpom-data-*")
	if err != nil {
		return err
	}
	tmpName := tmp.Name()
	defer func() { _ = os.Remove(tmpName) }()
	if _, err := tmp.Write(data); err != nil {
		_ = tmp.Close()
		return err
	}
	if err := tmp.Sync(); err != nil {
		_ = tmp.Close()
		return err
	}
	if err := tmp.Close(); err != nil {
		return err
	}
	if err := os.Chmod(tmpName, 0o644); err != nil {
		return err
	}
	return os.Rename(tmpName, path)
}

// stringField reads a string field from a decoded JSON object.
func stringField(value map[string]any, key string) string {
	if value == nil {
		return ""
	}
	switch item := value[key].(type) {
	case string:
		return item
	case json.Number:
		return item.String()
	case bool:
		if item {
			return "true"
		}
		return "false"
	case float64:
		return fmt.Sprintf("%v", item)
	default:
		return ""
	}
}

// intField reads an integer field from a decoded JSON object.
func intField(value map[string]any, key string) int64 {
	if value == nil {
		return 0
	}
	switch item := value[key].(type) {
	case json.Number:
		if parsed, err := item.Int64(); err == nil {
			return parsed
		}
		if parsed, err := item.Float64(); err == nil {
			return int64(parsed)
		}
	case float64:
		return int64(item)
	case int:
		return int64(item)
	case int64:
		return item
	case string:
		var parsed json.Number = json.Number(item)
		if value, err := parsed.Int64(); err == nil {
			return value
		}
	}
	return 0
}

// boolField reads a boolean field from a decoded JSON object.
func boolField(value map[string]any, key string) bool {
	if value == nil {
		return false
	}
	switch item := value[key].(type) {
	case bool:
		return item
	case string:
		return item == "true" || item == "1"
	case json.Number:
		return item.String() != "0" && item.String() != ""
	case float64:
		return item != 0
	}
	return false
}

// cloneObject returns a shallow copy of a decoded JSON object.
func cloneObject(value map[string]any) map[string]any {
	clone := make(map[string]any, len(value)+4)
	for key, item := range value {
		clone[key] = item
	}
	return clone
}
