# DNS 服务器问题

> 最后更新：2026-05-24

---

## 症状

```
curl: (6) Could not resolve host
```

---

## 修复

```bash
# 换 DNS
# /etc/resolv.conf
nameserver 8.8.8.8
nameserver 223.5.5.5  # 阿里
```

---

## 相关

- [域名不存在/](../域名不存在/INDEX.md)
- [本地DNS缓存/](../本地DNS缓存/INDEX.md)
