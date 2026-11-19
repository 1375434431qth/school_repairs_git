<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" aria-hidden="true">
    <g v-html="content"></g>
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 }
})

const ICONS = {
  bell: '<path d="M12 3a5 5 0 0 0-5 5v3.2L5 15.5h14l-2-4.3V8a5 5 0 0 0-5-5z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M10 18a2 2 0 0 0 4 0" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  user: '<circle cx="12" cy="8.5" r="3.5" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M5 20c.8-3.4 3.5-5.2 7-5.2s6.2 1.8 7 5.2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  users: '<circle cx="9" cy="8.5" r="3" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M3 19c.7-3 3-4.6 6-4.6s5.3 1.6 6 4.6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M15.5 6.2a3 3 0 0 1 0 5.6M17 14.8c2 .5 3.4 1.9 4 4.2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  wrench: '<path d="M14.7 6.3a4 4 0 0 0 5 5l-8.4 8.4a2.1 2.1 0 0 1-3-3l8.4-8.4a4 4 0 0 1-2-2z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>',
  clipboard: '<rect x="5" y="4.5" width="14" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M9 4.5h6v2.2H9z" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M8.5 10.5h7M8.5 14h7M8.5 17.2h4.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  chart: '<path d="M4 20V4M4 20h16" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M7.5 16v-4M11.5 16V8M15.5 16v-6M19.5 16V6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  building: '<path d="M5 20V6.5L12 3l7 3.5V20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M9 9.5h2M13 9.5h2M9 13h2M13 13h2M9 16.5h2M13 16.5h2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  clock: '<circle cx="12" cy="12" r="8" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M12 7.5V12l3 2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  logout: '<path d="M10 5H6.5A1.5 1.5 0 0 0 5 6.5v11A1.5 1.5 0 0 0 6.5 19H10" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M13 8l4 4-4 4M17 12H9" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>',
  plus: '<path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  search: '<circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M16 16l4 4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  phone: '<path d="M7 4.5h3l1.5 4-2 1.5a10 10 0 0 0 4.5 4.5l1.5-2 4 1.5v3a1.5 1.5 0 0 1-1.6 1.5C10.9 18 6 13.1 5.5 6.1A1.5 1.5 0 0 1 7 4.5z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>',
  shield: '<path d="M12 3l7 3v5.5c0 4.4-2.9 7.7-7 9.5-4.1-1.8-7-5.1-7-9.5V6l7-3z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>',
  star: '<path d="M12 4l2.4 4.9 5.4.8-3.9 3.8.9 5.4-4.8-2.5-4.8 2.5.9-5.4L4.2 9.7l5.4-.8z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>',
  refresh: '<path d="M19 12a7 7 0 1 1-2-4.9" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M19 4v4h-4" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>'
}

const content = computed(() => ICONS[props.name] || '')
</script>
