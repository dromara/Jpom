/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package store implements the JSON file persistence used by the Java agent
// (cn.hutool JsonFileUtil). Files must stay byte/schema compatible so a node can
// switch between the Java and Go agent on the same data directory.
package store

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"
)

// Store is a mutex protected JSON file.
type Store struct {
	path string
	mu   sync.Mutex
}

// New creates a store for path.
func New(path string) *Store {
	return &Store{path: path}
}

// Path returns the backing file path.
func (s *Store) Path() string { return s.path }

// Lock locks the store for a read-modify-write cycle.
func (s *Store) Lock() { s.mu.Lock() }

// Unlock releases the lock.
func (s *Store) Unlock() { s.mu.Unlock() }

// Read decodes the file into dst. A missing file leaves dst untouched and
// returns nil, matching the Java behaviour of an empty cache file.
func (s *Store) Read(dst any) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.readLocked(dst)
}

func (s *Store) readLocked(dst any) error {
	content, err := os.ReadFile(s.path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}
	if len(content) == 0 {
		return nil
	}
	return json.Unmarshal(content, dst)
}

// Write atomically replaces the file content.
func (s *Store) Write(value any) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.writeLocked(value)
}

func (s *Store) writeLocked(value any) error {
	content, err := json.Marshal(value)
	if err != nil {
		return err
	}
	return s.writeBytesLocked(content)
}

// WriteRaw atomically replaces the file content with already encoded bytes.
// It is used for files whose JSON is produced elsewhere (for example the remote
// version cache that must keep the upstream document layout).
func (s *Store) WriteRaw(content []byte) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.writeBytesLocked(content)
}

func (s *Store) writeBytesLocked(content []byte) error {
	dir := filepath.Dir(s.path)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	tmp, err := os.CreateTemp(dir, ".jpom-store-*")
	if err != nil {
		return err
	}
	tmpName := tmp.Name()
	defer func() { _ = os.Remove(tmpName) }()
	if _, err := tmp.Write(content); err != nil {
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
	return os.Rename(tmpName, s.path)
}

// Update runs fn while holding the write lock and persists the mutated value.
// fn receives the current decoded value (created by newValue when the file is
// missing) and must return the value to persist.
func Update[T any](s *Store, newValue func() T, fn func(value *T) error) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	value := newValue()
	if err := s.readLocked(&value); err != nil {
		return fmt.Errorf("读取 %s 失败: %w", filepath.Base(s.path), err)
	}
	if err := fn(&value); err != nil {
		return err
	}
	return s.writeLocked(value)
}
