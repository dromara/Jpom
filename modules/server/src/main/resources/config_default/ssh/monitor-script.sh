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

free -m | grep Mem | awk 'BEGIN{print "total used free buff_cache available"} {print $2,$3,$4,$6,$7}'

LANG=C lscpu | awk -F: '/Model name/ {print $2}'
awk '/processor/{core++} END{print core}' /proc/cpuinfo
uptime | sed 's/,/ /g' | awk '{for(i=NF-2;i<=NF;i++)print $i }' | xargs
vmstat 1 1 | awk 'NR==3{print $11}'
vmstat 1 1 | awk 'NR==3{print $12}'
