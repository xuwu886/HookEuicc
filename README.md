# HookEuicc
解除某些软件对eSIM的要求，也可用于获取eSIM代码

在某些不直接提供eSIM激活码的应用，将会复制激活码到剪切板

## 其他功能
### 我不是eUICC卡
可用于部分设备与eUICC卡[ISD-R]独占，导致无法访问的问题。

**例如:** 在某些高通设备上使用自焊的 st33卡

> [!NOTE]
> 需勾选 `com.android.phone` 并无法热插拔卡，请插入Euicc卡后再重启手机

### OMAPI Bypass
绕过 ARA、ARF 限制。通常用于无ARA的卡进行OMAPI访问

> [!NOTE]
> 需勾选 `com.android.se` 并重启手机 (或执行 `su -c killall com.android.se`)
