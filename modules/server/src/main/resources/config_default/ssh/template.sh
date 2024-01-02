#!/bin/bash
#
# The MIT License (MIT)
#
# Copyright (c) 2019 Code Technology Studio
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#

# Mistakenly deleted !!!!!!!!!!!
# Init script templates for local build, local publish, script template, ssh publish, ssh command template and other related functions

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
