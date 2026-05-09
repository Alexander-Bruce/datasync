import { app, shell, BrowserWindow, ipcMain, dialog } from 'electron'
import path from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'

let mainWindow

function createWindow() {
  mainWindow = new BrowserWindow({
    title: 'Datasync',
    width: 900,
    height: 670,
    show: false,
    autoHideMenuBar: true,
    icon,
    webPreferences: {
      preload: path.join(__dirname, '../preload/index.js'),
      sandbox: false,
      webSecurity: false,
      contextIsolation: false,
      nodeIntegration: true
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(() => {
  electronApp.setAppUserModelId('com.datasync')

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // IPC test
  ipcMain.on('ping', () => console.log('pong'))

  createWindow()

  // 用于打开文件夹选择对话框
  ipcMain.handle('select-folder', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
      title: '选择同步文件夹',
      properties: ['openDirectory', 'createDirectory'],
      buttonLabel: '选择文件夹'
    })

    if (canceled || filePaths.length === 0) {
      return null
    }
    return filePaths[0]
  })

  ipcMain.on('open-file', (event, payload) => {
    if (typeof payload === 'object' && payload.basePath && payload.relativePath) {
      const fullPath = path.join(payload.basePath, payload.relativePath)
      shell.openPath(fullPath).catch((err) => {
        console.error(`[Main Process] 打开文件失败: ${fullPath}`, err)
      })
    }
  })
  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
