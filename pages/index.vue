<template>
  <div class="container">
    <div>
      <h1 class="title">
        mirelplatform
      </h1>
      <h2 class="subtitle">
        FRONT SERVICE
      </h2>
      <div class="links">
        <nuxt-link
          to="/mste"
          class="button--grey"
        >
          ProMarker
        </nuxt-link>
        <nuxt-link
          to="/apprunner"
          class="button--grey"
        >
          AppRunner
        </nuxt-link>
        <nuxt-link
          to="/sampleui"
          class="button--grey"
        >
          Sample UI
        </nuxt-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  layout: 'Main',
  components: {
  }
}
</script>
