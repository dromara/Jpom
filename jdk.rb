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
`git clone https://${gitee_user}:${gitee_token}@gitee.com/dromara/Jpom.git --branch download_link`
`cd Jpom && rm -rf jdk/*`
results.each do |result|
  FileUtils.mkdir_p "Jpom/jdk/#{result[:version]}"
  `cd Jpom && echo "#{result[:path]}" > jdk/#{result[:version]}/#{result[:arch]}`
end
`git config --global user.email "#{ENV['gitee_email']}" && git config --global user.name "jpom jdk"`
`cd Jpom && git add . && git commit -m "update jdk" && git push origin download_link`
