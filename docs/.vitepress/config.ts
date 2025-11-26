import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'ThorWallpaperTool',
  description: 'ThorWallpaperTool is an intelligent wallpaper processing tool specifically designed for Thor dual-screen handheld gaming devices, perfectly adapting to upper and lower screens with PPI optimization and customizable gap settings.',
  lang: 'zh-CN',
  
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '下载安装', link: '/download' },
      {
        text: '更多',
        items: [
          { text: 'GitHub', link: 'https://github.com/adam-ikari/ThorWallpaperTool' }
        ]
      }
    ],

    sidebar: [
      {
        text: '导航',
        items: [
          { text: '首页', link: '/' },
          { text: '下载安装', link: '/download' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/adam-ikari/ThorWallpaperTool' }
    ],

    footer: {
      message: 'Released under MIT License',
      copyright: 'Copyright © 2024 ThorWallpaperTool'
    },

    search: {
      provider: 'local'
    }
  },

  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }],
    ['meta', { name: 'theme-color', content: '#3c82f6' }]
  ]
})