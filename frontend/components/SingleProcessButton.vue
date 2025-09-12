<template>
  <button :disabled="disabled || processing" @click="process">
    <slot />
  </button>
</template>

<script>
export default {
  name: 'single-process-button',
  props: {
    onclick: {
      type: Function,
      required: true
    },
    disabled: {
      type: Boolean,
      default: false
    },
  },
  data() {
    return {
      processing: false
    }
  },
  methods: {
    process (event) {
      if (this.processing) {
        return;
      }
      this.onClick(event)
        .then(() => {
          then.processing = false;
        })
    }
  }
}
</script>