import type { Config } from "tailwindcss";
import defaultTheme from "tailwindcss/defaultTheme";

export default {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "#f6f7f2",
        paper: "#ffffff",
        ink: "#20322b",
        muted: "#718078",
        line: "#e7ebe5",
        pine: {
          50: "#f2f8f5",
          100: "#e4f1ea",
          500: "#397b63",
          600: "#2f6854",
          700: "#285746",
        },
        clay: {
          50: "#fff7f3",
          500: "#b96d51",
          600: "#9f5941",
        },
      },
      fontFamily: {
        sans: ['"Noto Sans SC"', '"Microsoft YaHei"', ...defaultTheme.fontFamily.sans],
        display: ['"Noto Serif SC"', '"Songti SC"', "serif"],
      },
      boxShadow: {
        card: "0 18px 50px rgba(45, 73, 60, 0.08)",
      },
      borderRadius: {
        xl: "1rem",
        "2xl": "1.35rem",
      },
    },
  },
  plugins: [],
} satisfies Config;
