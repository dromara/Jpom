#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#
# 通过 Adoptium API 获取最新 GA 文件名，再拼清华 TUNA 直链，刷新 jdk 下载清单；
# 由 Gitee Go 定时流水线 .workflow/ruby-jdk.yml 每天调用。
#
# 与旧版的差异：
#   1. 以 Adoptium API 的“最新 GA 文件名”为准，再拼清华 TUNA 直链（不再依赖 TUNA 目录 HTML 的 XPath；
#      TUNA 目录页对脚本访问可能返回 403，旧版会直接抛异常）。
#   2. 每个 URL 用 HEAD 校验 200 才写入；解析失败时保留旧值并显式告警，不会 `rm -rf jdk/*` 后推空清单。
#   3. 只在内容变化时 commit/push；无变化直接退出 0（避免触发 push 回环、浪费 Gitee Go 构建时长）。
#   4. push 失败重试，最终失败以非 0 退出，让流水线红掉而不是静默停更。

require 'httparty'
require 'nokogiri'
require 'json'
require 'fileutils'

TUNA_BASE = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium'
ADOPTIUM_API = 'https://api.adoptium.net/v3/assets/latest/%<version>s/hotspot?os=linux&architecture=%<arch>s&image_type=jdk&vendor=eclipse'
# 与 server 侧 DockerYmlDsl.javaPluginCheck 白名单对齐（8/11/17/18/21/25），并保留清单中已有的 19/20
VERSIONS = %w[8 11 17 18 19 20 21 25]
ARCHS = %w[aarch64 arm ppc64le s390x x64]
WORKDIR = File.expand_path(__dir__)
JDK_DIR = File.join(WORKDIR, 'jdk')
UA = 'jpom-jdk-refresh/2.0 (+https://gitee.com/dromara/Jpom)'

def http_get(url)
  HTTParty.get(url, headers: { 'User-Agent' => UA }, timeout: 30)
end

def url_alive?(url)
  resp = HTTParty.head(url, headers: { 'User-Agent' => UA }, timeout: 20)
  resp.code == 200
rescue StandardError
  false
end

# 主数据源：Adoptium API 直接给出最新 GA 的包文件名
def api_filename(version, arch)
  resp = http_get(format(ADOPTIUM_API, version: version, arch: arch))
  return nil unless resp.code == 200

  data = JSON.parse(resp.body)
  return nil if data.empty?

  data[0].dig('binary', 'package', 'name')
rescue StandardError => e
  warn "adoptium api #{version}/#{arch} failed: #{e.class}: #{e.message}"
  nil
end

# 兜底：TUNA 目录页第一个 tar.gz（带浏览器 UA，目录页结构未变时可用）
def tuna_listing_filename(version, arch)
  url = "#{TUNA_BASE}/#{version}/jdk/#{arch}/linux/"
  resp = http_get(url)
  return nil unless resp.code == 200

  doc = Nokogiri::HTML(resp.body)
  link = doc.xpath('//*[@id="list"]/tbody/tr[2]/td[1]/a').first
  return nil unless link

  name = link.text.strip
  name.end_with?('.tar.gz') ? name : nil
rescue StandardError => e
  warn "tuna listing #{version}/#{arch} failed: #{e.class}: #{e.message}"
  nil
end

def resolve_url(version, arch)
  candidates = [api_filename(version, arch), tuna_listing_filename(version, arch)].compact.uniq
  candidates.each do |name|
    url = "#{TUNA_BASE}/#{version}/jdk/#{arch}/linux/#{name}"
    return url if url_alive?(url)
    warn "candidate not reachable: #{url}"
  end
  nil
end

changed = 0
missing = []
VERSIONS.each do |version|
  ARCHS.each do |arch|
    file = File.join(JDK_DIR, version, arch)
    # 只更新清单里已存在的“版本/架构”组合，避免悄悄改变清单结构
    next unless File.exist?(file)

    old_url = File.read(file).strip
    url = resolve_url(version, arch)
    if url.nil?
      missing << "#{version}/#{arch}"
      warn "KEEP #{version}/#{arch} (old: #{old_url})"
      next
    end
    if url == old_url
      puts "OK   #{version}/#{arch} #{url}"
      next
    end

    File.write(file, "#{url}\n")
    changed += 1
    puts "UPD  #{version}/#{arch} #{url}"
  end
end

puts "changed files: #{changed}"
unless missing.empty?
  warn "unresolved: #{missing.join(', ')}（已保留旧值，请人工确认）"
end

system('git', 'add', 'jdk') || abort('git add jdk failed')
if `git status --porcelain jdk`.strip.empty?
  puts 'no jdk changes, skip commit/push'
  exit 0
end

email = ENV['gitee_email']
abort('ENV[gitee_email] missing: 请在 Gitee 仓库设置中恢复该变量') if email.nil? || email.strip.empty?
system('git', 'config', 'user.name', 'jpom jdk') || abort('git config user.name failed')
system('git', 'config', 'user.email', email) || abort('git config user.email failed')
system('git', 'commit', '-m', 'update jdk') || abort('git commit failed')

# 最多重试 3 次（并发/网络抖动）；失败以非 0 退出，让流水线显式失败
ok = false
3.times do |i|
  ok = system('git', 'push', 'origin', 'HEAD:download_link')
  break if ok

  sleep(5 * (i + 1))
end
abort('git push failed after retries') unless ok
puts 'pushed jdk manifest update'
