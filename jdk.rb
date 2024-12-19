#
# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
# 			http://license.coscl.org.cn/MulanPSL2
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PSL v2 for more details.
#

require 'httparty'
require 'nokogiri'
require 'fileutils'

def get_first_link(url)
  response = HTTParty.get url
  doc = Nokogiri::HTML response.body
  path = doc.xpath('//*[@id="list"]/tbody/tr[2]/td[1]/a').first.text
  "#{url}#{path}"
end

def get_versions
  response = HTTParty.get('https://mirrors.tuna.tsinghua.edu.cn/Adoptium/')
  doc = Nokogiri::HTML response.body
  versions = []
  doc.xpath('//*[@id="list"]/tbody/tr/td/a').each do |link|
    version = link.text.to_i
    versions << version if version != 0
  end
  versions
end
# versions = %w(8 11 17 18)
archs = %w(aarch64 arm ppc64 ppc64le s390x x32 x64)
results = []
get_versions.each do |version|
  archs.each do |arch|
    path = get_first_link("https://mirrors.tuna.tsinghua.edu.cn/Adoptium/#{version}/jdk/#{arch}/linux/") rescue nil
    results << { version: version, arch: arch, path: path } if path
  end
end
`rm -rf jdk/*`
results.each do |result|
  FileUtils.mkdir_p "jdk/#{result[:version]}"
  `echo "#{result[:path]}" > jdk/#{result[:version]}/#{result[:arch]}`
end
`git config --global user.email "#{ENV['gitee_email']}" && git config --global user.name "jpom jdk"`
`git add . && git commit -m "update jdk" && git push origin download_link`
