<template>
  <div>
    <common-header />
    <nuxt />
    <common-footer />
  </div>
</template>

<script>
import CommonHeader from '~/components/frame/Header.vue'
import CommonFooter from '~/components/frame/Footer.vue'

export default {
  name: 'Main',
  components: {
    CommonHeader,
    CommonFooter
  },
  methods: {
  }
}
</script>
