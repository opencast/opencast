// tailwind.config.js
export default {
    theme: {
        extend: {
            transitionProperty: {
                'height': 'height'
            },
            animation: {
                'pulse-fast': 'pulse 0.7s cubic-bezier(0.4, 0, 0.6, 1) infinite'
            }
        }
    },
    plugins: [],
};