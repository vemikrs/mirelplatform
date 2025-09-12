<template>
  <header>
    <b-navbar type="light" variant="faded">
      <b-navbar-brand href="#">
        mirelplatform
      </b-navbar-brand>
      <b-navbar-toggle target="nav-collapse" />
      <b-collapse id="nav-collapse" is-nav>
        <b-navbar-nav>
          <b-nav-item to="/">
            Top
          </b-nav-item>
          <b-nav-item to="/mste">
            ProMarker
          </b-nav-item>
          <b-nav-item to="/apprunner">
            AppRunner
          </b-nav-item>
          <b-nav-item to="/sampleui">
            Sample UI
          </b-nav-item>
        </b-navbar-nav>
      </b-collapse>
      <b-navbar-nav class="ml-auto">
        <b-nav-form>
          <b-form-input size="sm" class="mr-sm-4" placeholder="Command" />
          <b-button size="sm" class="my-2 my-sm-0" type="submit" hidden />
        </b-nav-form>

        <b-nav-item-dropdown right>
          <template v-slot:button-content>
            <span>Account</span>
          </template>
          <b-dropdown-item to="/signin">
            Sign In
          </b-dropdown-item>
        </b-nav-item-dropdown>
      </b-navbar-nav>
    </b-navbar>
  </header>
</template>

<script>
export default {
  name: 'CommonHeader'
}
</script>
