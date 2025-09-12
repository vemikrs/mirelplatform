<template>
  <div class="container">
    <div class="container_title">
      AppRunner
    </div>
    <div class="inner">
      <div class="rightitems">
        <b-button :disabled="disabled || processing" variant="secondary">
          📄全てクリア
        </b-button>
        <b-button v-b-modal.modal-psv-dialog :disabled="disabled || processing" variant="secondary">
          📎Yaml形式
        </b-button>
      </div>
      <hr>
      <div>
        <form ref="form1" @submit.stop.prevent="mainHandleSubmit">
          <b-form-group
            invalid-feedback="Required item error."
          >
            <b-container fluid>
              <legend>実行パラメータ</legend>
              <b-row v-for="eparam in eparams" :key="eparam.id" class="my-1">
                <b-col sm="3">
                  <label :for="`eparam-${eparam.id}`" class="pm_label">{{ eparam.name }}</label>
                </b-col>
                <b-col v-if="eparam.valueType=='file'" sm="1">
                  <b-button @click="fileUpload(eparam.id, eparam.value)">
                    📎
                  </b-button>
                </b-col>
                <b-col v-if="eparam.valueType=='file'" sm="3">
                  <b-form-input
                    :id="`eparam-${eparam.id}`"
                    v-model="eparam.value"
                    :placeholder="eparam.placeholder"
                    :disabled="true"
                    required
                  />
                </b-col>
                <b-col v-else sm="4">
                  <b-form-input
                    :id="`eparam-${eparam.id}`"
                    v-model="eparam.value"
                    :placeholder="eparam.placeholder"
                    :disabled="disabled || processing"
                    required
                  />
                </b-col>
                <b-col sm="5" class="fm_notes">
                  <span>{{ eparam.note }}</span>
                </b-col>
              </b-row>
            </b-container>
            <hr>
            <b-button :disabled="disabled || processing || serialNoNoSelected" @click="process()" variant="primary">
              Process
            </b-button>
            <hr>
          </b-form-group>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
// import BvUpload from '~/components/dialog/BvDownloadDialog.vue'

export default {
  layout: 'Main',
  components: {},
  data () {
    return {
      eparams: [
        {
          id: 'testId',
          name: 'テストID',
          valueType: 'text',
          value: '000-uitest',
          note: '実行するテストのIDを入力してください。'
        },
        {
          id: 'environmentId',
          name: '環境ID',
          valueType: 'text',
          value: 'IT',
          note: '実行するテスト環境のIDを入力してください。'
        }
      ]
    }
  },
  created () {
  },
  methods: {
    createRequest (body) {
      const pitems = {
        // stencilCategoy: body.fltStrStencilCategory.selected,
        // stencilCanonicalName: body.fltStrStencilCd.selected,
        // serialNo: body.fltStrSerialNo.selected
      }

      const assigned = Object.assign(body.eparams)
        .filter((item) => {
          return !item.noSend
        })
      for (const key in assigned) {
        pitems[assigned[key].id] = assigned[key].value
      }
      return pitems
    },
    process () {
      this.processing = true
      axios.post(
        `/mapi/apps/arr/api/runTest`,
        { content: this.createRequest(this) }
      ).then((resp) => {
        /* eslint-disable no-console */
        console.log(resp)
        /* eslint-enable no-console */
        if (!resp.data.model) {
          this.processing = false
          return
        }

        if (!resp.data.errs === false &&
          resp.data.errs.length > 0) {
          this.bvMsgBoxErr(resp.data.errs)
          this.processing = false
          return
        }

        if (!resp.data.model.files === false) {
          const paramFiles = []
          for (const key in resp.data.model.files) {
            paramFiles[key] = {
              fileId: resp.data.model.files[key][0],
              name: resp.data.model.files[key][1]
            }
          }
          this.$root.$emit('bv::show::modal', 'bv_dialog', { files: paramFiles })
        }
        this.processing = false
      }).catch((errors) => {
        this.bvMsgBoxErr(errors)
        this.processing = false
      })
    },

    bvMsgBoxErr (msgs) {
      if (!msgs || msgs === undefined) {
        msgs = 'エラーが発生しました。管理者に問い合わせてください。'
      }
      let converted = ''
      if (Array.isArray(msgs)) {
        for (const key in msgs) {
          converted += msgs[key]
          converted += ' '
        }
      } else {
        converted += msgs
      }

      this.$bvModal.msgBoxOk(converted, {
        title: 'Error',
        size: 'lg',
        okTitle: 'Close',
        headerBgVariant: 'danger',
        headerTextVariant: 'light',
        footerBgVariant: 'light',
        scrollable: true,
        centered: true
      })
    }

  }
}
</script>

<style lang="css" scoped>
#fm_notes {
  white-space: pre-wrap; /* なんでダメなんだぁ。 */
}
</style>
