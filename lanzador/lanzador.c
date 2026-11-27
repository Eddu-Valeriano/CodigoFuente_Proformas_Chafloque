/*
 * Lanzador de ProformaTaller en un solo archivo.
 *
 * El .exe final se arma así:  [este lanzador] + [ZIP con la carpeta ProformaTaller] + [cola de 16 bytes]
 * La cola guarda la posición donde empieza el ZIP (8 bytes) y la marca "PFTALLER" (8 bytes).
 *
 * Al ejecutarse:
 *   1. Si la aplicación ya está descomprimida en %LOCALAPPDATA%\ProformaTaller\<versión>, la abre.
 *   2. Si no, copia el ZIP a la carpeta temporal, lo descomprime con tar.exe (incluido en
 *      Windows 10/11; si no existe usa PowerShell) y luego abre ProformaTaller.exe.
 * No necesita Java instalado: la carpeta descomprimida trae su propio entorno de Java.
 */
#ifndef UNICODE
#define UNICODE
#endif
#define __USE_MINGW_ANSI_STDIO 1
#ifndef _UNICODE
#define _UNICODE
#endif
#include <windows.h>
#include <shlobj.h>
#include <stdio.h>
#include <wchar.h>

#ifndef VERSION_PAQUETE
#define VERSION_PAQUETE L"1.0"
#endif

static const char MARCA[8] = {'P','F','T','A','L','L','E','R'};
static HWND ventanaAviso = NULL;

static void error(const wchar_t *msg) {
    if (ventanaAviso) DestroyWindow(ventanaAviso);
    MessageBoxW(NULL, msg, L"ProformaTaller", MB_ICONERROR | MB_OK);
}

static int existe(const wchar_t *ruta) {
    DWORD a = GetFileAttributesW(ruta);
    return a != INVALID_FILE_ATTRIBUTES;
}

/* Crea todas las carpetas de una ruta (como mkdir -p). */
static void crearCarpetas(const wchar_t *ruta) {
    wchar_t tmp[MAX_PATH * 2];
    wcsncpy(tmp, ruta, MAX_PATH * 2 - 1);
    tmp[MAX_PATH * 2 - 1] = 0;
    for (wchar_t *p = tmp + 3; *p; p++) {
        if (*p == L'\\') { *p = 0; CreateDirectoryW(tmp, NULL); *p = L'\\'; }
    }
    CreateDirectoryW(tmp, NULL);
}

/* Ventana pequeña "Preparando…" mientras se descomprime la primera vez. */
static void mostrarAviso(HINSTANCE inst) {
    int w = 380, h = 90;
    int x = (GetSystemMetrics(SM_CXSCREEN) - w) / 2, y = (GetSystemMetrics(SM_CYSCREEN) - h) / 2;
    ventanaAviso = CreateWindowExW(WS_EX_TOPMOST | WS_EX_TOOLWINDOW, L"STATIC",
        L"\n  Preparando ProformaTaller por primera vez...\n  Esto tarda unos segundos.",
        WS_POPUP | WS_BORDER | WS_VISIBLE | SS_LEFT, x, y, w, h, NULL, NULL, inst, NULL);
    HFONT f = CreateFontW(-16, 0, 0, 0, FW_NORMAL, 0, 0, 0, DEFAULT_CHARSET, 0, 0, CLEARTYPE_QUALITY, 0, L"Segoe UI");
    SendMessageW(ventanaAviso, WM_SETFONT, (WPARAM)f, TRUE);
    UpdateWindow(ventanaAviso);
}

/* Ejecuta un comando oculto y espera a que termine, atendiendo los mensajes de la ventana. */
static DWORD ejecutarYEsperar(wchar_t *cmd) {
    STARTUPINFOW si = { sizeof(si) };
    PROCESS_INFORMATION pi;
    if (!CreateProcessW(NULL, cmd, NULL, NULL, FALSE, CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) return (DWORD)-1;
    for (;;) {
        DWORD r = MsgWaitForMultipleObjects(1, &pi.hProcess, FALSE, INFINITE, QS_ALLINPUT);
        if (r == WAIT_OBJECT_0) break;
        MSG m;
        while (PeekMessageW(&m, NULL, 0, 0, PM_REMOVE)) { TranslateMessage(&m); DispatchMessageW(&m); }
    }
    DWORD code = 1;
    GetExitCodeProcess(pi.hProcess, &code);
    CloseHandle(pi.hProcess); CloseHandle(pi.hThread);
    return code;
}

/* Copia el ZIP incrustado (desde 'inicio' hasta 'fin') a un archivo aparte. */
static int extraerZip(const wchar_t *yo, LONGLONG inicio, LONGLONG fin, const wchar_t *destino) {
    HANDLE in = CreateFileW(yo, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
    if (in == INVALID_HANDLE_VALUE) return 0;
    HANDLE out = CreateFileW(destino, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, 0, NULL);
    if (out == INVALID_HANDLE_VALUE) { CloseHandle(in); return 0; }
    LARGE_INTEGER pos; pos.QuadPart = inicio;
    SetFilePointerEx(in, pos, NULL, FILE_BEGIN);
    static char buf[1 << 20];
    LONGLONG resta = fin - inicio;
    int ok = 1;
    while (resta > 0) {
        DWORD n = (DWORD)(resta > (LONGLONG)sizeof(buf) ? sizeof(buf) : resta), leidos = 0, escritos = 0;
        if (!ReadFile(in, buf, n, &leidos, NULL) || leidos == 0) { ok = 0; break; }
        if (!WriteFile(out, buf, leidos, &escritos, NULL) || escritos != leidos) { ok = 0; break; }
        resta -= leidos;
    }
    CloseHandle(in); CloseHandle(out);
    return ok;
}

int WINAPI wWinMain(HINSTANCE inst, HINSTANCE prev, PWSTR args, int show) {
    (void)prev; (void)show;
    wchar_t yo[MAX_PATH];
    GetModuleFileNameW(NULL, yo, MAX_PATH);

    /* Leer la cola: posición del ZIP + marca */
    HANDLE f = CreateFileW(yo, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
    if (f == INVALID_HANDLE_VALUE) { error(L"No se pudo leer el ejecutable."); return 1; }
    LARGE_INTEGER tam; GetFileSizeEx(f, &tam);
    LARGE_INTEGER pos; pos.QuadPart = tam.QuadPart - 16;
    SetFilePointerEx(f, pos, NULL, FILE_BEGIN);
    unsigned char cola[16]; DWORD leidos = 0;
    ReadFile(f, cola, 16, &leidos, NULL);
    CloseHandle(f);
    if (leidos != 16 || memcmp(cola + 8, MARCA, 8) != 0) { error(L"El ejecutable está incompleto o dañado."); return 1; }
    LONGLONG inicio = 0;
    for (int i = 7; i >= 0; i--) inicio = (inicio << 8) | cola[i];
    LONGLONG fin = tam.QuadPart - 16;

    /* Carpeta interna: %LOCALAPPDATA%\ProformaTaller\<versión>-<tamaño> */
    wchar_t local[MAX_PATH], base[MAX_PATH * 2], app[MAX_PATH * 2], marca[MAX_PATH * 2];
    if (FAILED(SHGetFolderPathW(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, local))) { error(L"No se encontró la carpeta LocalAppData."); return 1; }
    swprintf(base, MAX_PATH * 2, L"%ls\\ProformaTaller\\v%ls-%lld", local, VERSION_PAQUETE, (long long)(fin - inicio));
    swprintf(app, MAX_PATH * 2, L"%ls\\ProformaTaller\\ProformaTaller.exe", base);
    swprintf(marca, MAX_PATH * 2, L"%ls\\listo.txt", base);

    if (!existe(app) || !existe(marca)) {
        mostrarAviso(inst);
        crearCarpetas(base);
        wchar_t temp[MAX_PATH], zip[MAX_PATH * 2], cmd[MAX_PATH * 6], sys[MAX_PATH], tar[MAX_PATH * 2];
        GetTempPathW(MAX_PATH, temp);
        swprintf(zip, MAX_PATH * 2, L"%lsproformataller_%lld.zip", temp, (long long)(fin - inicio));
        if (!extraerZip(yo, inicio, fin, zip)) { error(L"No se pudo preparar la aplicación (sin espacio en la carpeta temporal)."); return 1; }

        GetSystemDirectoryW(sys, MAX_PATH);
        swprintf(tar, MAX_PATH * 2, L"%ls\\tar.exe", sys);
        DWORD r = (DWORD)-1;
        if (existe(tar)) {
            swprintf(cmd, MAX_PATH * 6, L"\"%ls\" -xf \"%ls\" -C \"%ls\"", tar, zip, base);
            r = ejecutarYEsperar(cmd);
        }
        if (r != 0) { /* Alternativa: PowerShell */
            swprintf(cmd, MAX_PATH * 6,
                L"powershell.exe -NoProfile -ExecutionPolicy Bypass -Command \"Expand-Archive -LiteralPath '%ls' -DestinationPath '%ls' -Force\"",
                zip, base);
            r = ejecutarYEsperar(cmd);
        }
        DeleteFileW(zip);
        if (r != 0 || !existe(app)) { error(L"No se pudo descomprimir la aplicación."); return 1; }
        HANDLE m = CreateFileW(marca, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, 0, NULL);
        if (m != INVALID_HANDLE_VALUE) CloseHandle(m);
        if (ventanaAviso) { DestroyWindow(ventanaAviso); ventanaAviso = NULL; }
    }

    /* Abrir la aplicación con su carpeta como directorio de trabajo */
    wchar_t dir[MAX_PATH * 2], linea[MAX_PATH * 4];
    swprintf(dir, MAX_PATH * 2, L"%ls\\ProformaTaller", base);
    swprintf(linea, MAX_PATH * 4, L"\"%ls\" %ls", app, args ? args : L"");
    STARTUPINFOW si = { sizeof(si) };
    PROCESS_INFORMATION pi;
    if (!CreateProcessW(NULL, linea, NULL, NULL, FALSE, 0, NULL, dir, &si, &pi)) {
        error(L"No se pudo iniciar ProformaTaller.");
        return 1;
    }
    CloseHandle(pi.hProcess); CloseHandle(pi.hThread);
    return 0;
}
