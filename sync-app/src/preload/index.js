import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'

if (process.contextIsolated) {
  try {
    // 显式暴露 ipcRenderer 给渲染进程
    contextBridge.exposeInMainWorld('electron', {
      ...electronAPI,
      ipcRenderer: {
        invoke: (channel, ...args) => ipcRenderer.invoke(channel, ...args)
      }
    })
  } catch (error) {
    console.error(error)
  }
} else {
  window.electron = electronAPI
  // 非隔离环境也需要确保 ipcRenderer 存在
  window.electron.ipcRenderer = ipcRenderer
}
