cd /d %~dp0

prunsrv //IS//file-backup --DisplayName="file-backup" ^
  --Jvm=%~dp0jre\bin\client\jvm.dll ^
  --Classpath=%~dp0file-backup-0.1.0-SNAPSHOT.jar ^
  --StartMode=jvm --StartClass=com.example.filebackup.FileBackupApplication --StartMethod=main --StartParams=start ^
  --StopMode=jvm --StopClass=com.example.filebackup.FileBackupApplication --StopMethod=main --StopParams=stop --StopTimeout=10 ^
  --LogPath=%~dp0logs --StdOutput=auto --StdError=auto

pause
