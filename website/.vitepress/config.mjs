import { defineConfig } from "vitepress";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "索尔双屏掌机壁纸工具",
  description:
    "ThorWallpaperTool is an intelligent wallpaper processing tool specifically designed for Thor dual-screen handheld gaming devices, perfectly",
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [{ text: "首页", link: "/" }],

    sidebar: [],

    socialLinks: [
      {
        icon: "github",
        link: "https://github.com/adam-ikari/ThorWallpaperTool",
      },
    ],
  },
});
