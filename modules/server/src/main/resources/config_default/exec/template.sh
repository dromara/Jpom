#!/bin/bash
#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

# Mistakenly deleted !!!!!!!!!!!
# Init script templates for local build, local publish, script template and other related functions

user="$(id -un 2>/dev/null || true)"

if [ "$user" == 'root' ]; then
	rootProfiles=("/etc/profile" "/etc/bashrc")
	for element in "${rootProfiles[@]}"; do
		if [ -f "$element" ]; then
			# shellcheck disable=SC1090
			source "$element"
		fi
	done
fi

userProfiles=("$HOME/.bash_profile" "$HOME/.bashrc" "$HOME/.bash_login")
for element in "${userProfiles[@]}"; do
	if [ -f "$element" ]; then
		# shellcheck disable=SC1090
		source "$element"
	fi
done

# Do not delete the following content (leave at least one blank line)! ! ! ! !
