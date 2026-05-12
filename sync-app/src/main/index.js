import { app, shell, BrowserWindow, ipcMain, dialog } from 'electron'
import path from 'path'
import { existsSync } from 'fs'
import { spawn } from 'child_process'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'

let mainWindow
let backendProcess

function getBackendJarPath() {
  if (is.dev) {
    return path.resolve(__dirname, '../../../client-app/target/dataSync-server-0.0.1-SNAPSHOT.jar')
  }
  return path.join(process.resourcesPath, 'backend', 'client-app.jar')
}

function getJavaExecutable() {
  const executable = process.platform === 'win32' ? 'java.exe' : 'java'
  const bundledJava = path.join(process.resourcesPath, 'jre', 'bin', executable)
  return existsSync(bundledJava) ? bundledJava : 'java'
}

function startClientBackend() {
  const jarPath = getBackendJarPath()
  if (!existsSync(jarPath)) {
    console.warn(`[DataSync] Client backend jar not found: ${jarPath}`)
    return
  }

  backendProcess = spawn(getJavaExecutable(), ['-jar', jarPath], {
    stdio: 'ignore',
    windowsHide: true
  })

  backendProcess.on('error', (error) => {
    console.error('[DataSync] Failed to start client backend:', error)
  })

  backendProcess.on('exit', () => {
    backendProcess = null
  })
}

function stopClientBackend() {
  if (backendProcess && !backendProcess.killed) {
    backendProcess.kill()
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    title: 'Datasync',
    width: 900,
    height: 670,
    minWidth: 760,
    minHeight: 560,
    show: false,
    autoHideMenuBar: true,
    icon,
    webPreferences: {
      preload: path.join(__dirname, '../preload/index.js'),
      sandbox: false,
      webSecurity: false,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  if (is.dev && process.env.ELECTRON_RENDERER_URL) {
    mainWindow.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(() => {
  electronApp.setAppUserModelId('com.datasync')

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  ipcMain.on('ping', () => console.log('pong'))

  ipcMain.handle('select-folder', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
      title: 'Select sync folder',
      properties: ['openDirectory', 'createDirectory'],
      buttonLabel: 'Select folder'
    })

    if (canceled || filePaths.length === 0) {
      return null
    }
    return filePaths[0]
  })

  ipcMain.on('open-file', (_, payload) => {
    if (typeof payload === 'object' && payload.basePath && payload.relativePath) {
      const fullPath = path.join(payload.basePath, payload.relativePath)
      shell.openPath(fullPath).catch((err) => {
        console.error(`[DataSync] Failed to open file: ${fullPath}`, err)
      })
    }
  })

  startClientBackend()
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('before-quit', stopClientBackend)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
