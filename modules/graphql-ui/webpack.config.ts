import * as path from "path";
import { CallableOption } from "webpack-cli";
import HtmlWebpackPlugin from "html-webpack-plugin";
import ESLintPlugin from "eslint-webpack-plugin";

const config: CallableOption = (_env, argv) => {

  const isProd = argv.mode === "production";

  return {
    mode: isProd ? "production" : "development",
    context: path.join(__dirname, "/src/main/webapp/ui"),
    entry: "./index.tsx",
    output: {
      filename: "main.js",
      path: path.resolve(__dirname, "target/classes"),
    },
    resolve: {
      extensions: [".tsx", ".ts", ".js"],
    },
    module: {
      rules: [
        {
          test: /\.(ts|tsx)$/,
          exclude: /node_modules/,
          use: ["babel-loader"],
        },
        {
          test: /\.css$/,
          use: ["style-loader", "css-loader"],
        },
      ],
    },
    plugins: [
      new ESLintPlugin({
        extensions: ["ts", "tsx", "js"],
      }),
      new HtmlWebpackPlugin({
        template: "../index.html",
      }),
    ],
  };
};

export default config;
