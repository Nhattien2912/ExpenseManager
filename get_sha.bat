@echo off
"C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "C:\Users\clubb\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android > sha_result.txt 2>&1
echo Done >> sha_result.txt
type sha_result.txt
pause
