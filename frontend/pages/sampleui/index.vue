<template>
  <div class="container">
    <div>
      <h2 class="subtitle">
        Nuxt.js Sample - ag-grid
      </h2>
      <hr>
      <div>
        <demo-ag-grid-1 />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import DemoAgGrid1 from '~/components/pane/DemoAgGrid1.vue'
export default {
  layout: 'Main',
  components: {
    DemoAgGrid1
  }
}
</script>
