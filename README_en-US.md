# HookEuicc

Bypasses eSIM requirements for specific applications and can be used to retrieve eSIM activation codes.

For apps that do not directly display the eSIM activation code, this module will automatically copy the code to your clipboard.

## Additional Features

### OMAPI Bypass

Bypasses ARA (Access Rule Application) and ARF (Access Rule File) restrictions. 
Typically used to grant OMAPI access to cards that lack ARA.

> [!NOTE]
> You must select `com.android.se` in the scope and reboot (or run: `su -c killall com.android.se`).
> Do not enable this unless specifically required.
