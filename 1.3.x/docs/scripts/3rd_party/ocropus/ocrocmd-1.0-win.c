#define _CRT_SECURE_NO_WARNINGS
#pragma warning(disable:4995)
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tchar.h>
#include <stdio.h>
#include <stdlib.h>
#include <process.h>

#define _MAX_CMD 32768

int _tmain(int argc, TCHAR *argv[], TCHAR *envp[])
{
  int ii, len, arg_len;
  TCHAR cmd[_MAX_CMD*2];
  PROCESS_INFORMATION processInfo;
  STARTUPINFO startupInfo;
  BOOL result;
  DWORD error, status, exitCode;

  _tcscpy(cmd, _T("ocropus"));
  _tcscat(cmd, _T(" page"));
  len = _tcslen(cmd);

  for (ii = 1; ii < argc && len < _MAX_CMD; ii++) {
    arg_len = _tcslen(argv[ii]) + sizeof(TCHAR);
    if ((len + arg_len) > _MAX_CMD) break;
    _tcscat(cmd, _T(" "));
    _tcscat(cmd, argv[ii]);
    len = _tcslen(cmd);
  }

  memset(&processInfo, 0, sizeof(processInfo));
  memset(&startupInfo, 0, sizeof(startupInfo));
  startupInfo.cb = sizeof(startupInfo);

  result = CreateProcess(
             NULL,  // LPCTSTR lpApplicationName
             (LPTSTR)&cmd, // LPTSTR lpCommandLine
             NULL,  // LPSECURITY_ATTRIBUTES lpProcessAttributes
             NULL,  // LPSECURITY_ATTRIBUTES lpThreadAttributes
             TRUE,  // BOOL bInheritHandles
             NORMAL_PRIORITY_CLASS, // DWORD dwCreationFlags
             NULL,  // LPVOID lpEnvironment
             NULL,  // LPCTSTR lpCurrentDirectory
             &startupInfo,  // LPSTARTUPINFO lpStartupInfo
             &processInfo); // LPPROCESS_INFORMATION lpProcessInformation
  if (!result) {
    error = GetLastError();
    perror("CreateProcess");
    exit(error);
  }

  status = WaitForSingleObject(processInfo.hProcess, INFINITE);
  if (status == WAIT_FAILED) {
    error = GetLastError();
    perror("WaitForSingleObject");
    exit(error);
  }

  exitCode = 0;
  result = GetExitCodeProcess(processInfo.hProcess, &exitCode);
  if (!result) {
    error = GetLastError();
    CloseHandle(processInfo.hProcess);
    CloseHandle(processInfo.hThread);
    perror("GetExitCodeProcess");
    exit(error);
  }
  CloseHandle(processInfo.hProcess);
  CloseHandle(processInfo.hThread);

  return exitCode;
} // main
