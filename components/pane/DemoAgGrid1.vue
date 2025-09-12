<template>
  <ag-grid-vue
    :columnDefs="columnDefs"
    :rowData="rowData"
    :modules="modules"
    style="width: auto; height: 300px;"
    class="ag-theme-material"
  />
</template>

<script>
// Modules.
import { AgGridVue } from '@ag-grid-community/vue'
import { AllCommunityModules } from '@ag-grid-community/all-modules'

// Styles.
import '@/node_modules/@ag-grid-community/core/dist/styles/ag-grid.css'
import '@/node_modules/@ag-grid-community/core/dist/styles/ag-theme-alpine.css'
import '@/node_modules/@ag-grid-community/core/dist/styles/ag-theme-material.css'

// Script.
export default {
  name: 'DemoAgGrid1',
  components: {
    AgGridVue
  },
  data () {
    return {
      columnDefs:
      [
        { field: 'make', checkboxSelection: true },
        { field: 'model' },
        { field: 'price' }
      ],
      rowData:
      [
        { 'make': 'Toyota', 'model': 'Celica', 'price': 35000 },
        { 'make': 'Ford', 'model': 'Mondeo', 'price': 32000 },
        { 'make': 'Porsche', 'model': 'Boxter', 'price': 72000 }
      ],
      modules: AllCommunityModules
    }
  }
}
</script>
