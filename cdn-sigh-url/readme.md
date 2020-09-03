# GCP cloud CDN sign URLs and cookies

Test env

```
http://<your_domain_or_ip>/1.txt
http://<your_domain_or_ip>/2.txt
http://<your_domain_or_ip>/3.txt
http://<your_domain_or_ip>/cdn1/1.txt
http://<your_domain_or_ip>/cdn1/2.txt
http://<your_domain_or_ip>/cdn1/3.txt
```

Create sign key in Cloud CDN and set the environment, replace the key_name, key_code, url, url_prefix in test.py

[GCP setup](https://cloud.google.com/cdn/docs/using-signed-urls)
