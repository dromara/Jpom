   ###  Jpom使用Nginx代理推荐配置

```
server {
    #charset koi8-r;
    access_log  /var/log/nginx/jpom.log main;
    listen       80;
    server_name  jpom.xxxxxx.cn;
    
    location / {
        proxy_pass   http://127.0.0.1:2122/;
        proxy_set_header Host      $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size  50000m;
        client_body_buffer_size 128k;
        #  websocket 配置
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        #  代理的二级路径配置 值填写nginx 中location的值  如 location /test-path/ {}
        #  proxy_set_header Jpom-ProxyPath      /test-path/;
    }
}
```

### Https 推荐配置

```
server {
	listen 443;
	server_name jpom.xxxxxx.cn;
	access_log  /var/log/nginx/jpom.log main;
	ssl on;
	ssl_certificate   /etc/nginx/ssl/jpom-xxxxxx.pem;
	ssl_certificate_key  /etc/nginx/ssl/jpom-xxxxxx.key;
	ssl_session_timeout 5m;
	ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;
	ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
	ssl_prefer_server_ciphers on;
	
	location / {
		proxy_pass   http://127.0.0.1:2122/;
		# 代理的二级路径配置 值填写nginx 中location的值  如 location /test-path/ {}
		# proxy_set_header Jpom-ProxyPath      /;
		proxy_set_header Host      $host;
		proxy_set_header X-Real-IP $remote_addr;
		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
		client_max_body_size  50000m;
		client_body_buffer_size 128k;
		#	websocket 配置
		proxy_set_header Upgrade $http_upgrade;
		proxy_set_header Connection "upgrade";
	}
}

server {
    #charset koi8-r;
    listen       80;
    server_name  jpom.xxxxxx.cn;
    rewrite ^(.*)$  https://$host$1 permanent;
}
```