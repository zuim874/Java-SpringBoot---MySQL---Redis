# 安装nginx: https://nginx.org/en/download.html
# 修改/nginx/conf/nginx.conf
# 新增api_limit.conf
（确保springboot运行在8080端口）

# cmd
nginx -t
# 确保输出
    nginx: the configuration file D:\nginx-1.30.4/conf/nginx.conf syntax is ok
    nginx: configuration file D:\nginx-1.30.4/conf/nginx.conf test is successful
nginx -s reload
nginx
# 此时多次并行尝试会阻断连接，实现nginx限流