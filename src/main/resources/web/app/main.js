import Chart from 'chart.js/auto'

// Expose Chart globally so plain scripts like /app.js can use it without imports
// eslint-disable-next-line no-undef
;(globalThis || window).Chart = Chart

export { Chart }
