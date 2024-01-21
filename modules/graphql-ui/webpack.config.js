const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = (env, argv) => {
    "use strict";
    const isProd = argv.mode === 'production';

    return {
        mode: isProd ? 'production' : 'development',
        context: path.join(__dirname, '/src/main/webapp/ui'),
        entry: './index.tsx',
        output: {
            filename: 'main.js',
            path: path.resolve(__dirname, 'target/classes')
        },
        resolve: {
            extensions: ['.tsx', '.ts', '.js']
        },
        module: {
            rules: [
                {
                    test: /\.(ts|tsx)$/,
                    exclude: /node_modules/,
                    use: ['babel-loader']
                },
                {
                    test: /\.css$/,
                    use: ['style-loader', 'css-loader']
                }
            ]
        },
        plugins: [
            new HtmlWebpackPlugin({
                template: '../index.html'
            })
        ]
    };
};
