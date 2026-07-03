/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef4ff",
          100: "#dbe6fe",
          500: "#3b5fe0",
          600: "#2d4bc4",
          700: "#233ca0",
        },
      },
    },
  },
  plugins: [],
};
