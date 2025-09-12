<template>
  <div class="container">
    <div class="container_title">
      ProMarker 払出画面
    </div>
    <div class="inner">
      <div class="rightitems">
        <b-button :disabled="disabled || processing || stencilNoSelected" @click="clearDelems()" variant="secondary">
          📄ステンシル定義を再取得
        </b-button>
        <b-button :disabled="disabled || processing" @click="clearAll()" variant="secondary">
          📄全てクリア
        </b-button>
        <b-button v-b-modal.modal-psv-dialog :disabled="disabled || processing" variant="secondary">
          📎Json形式
        </b-button>
        <b-button :disabled="disabled || processing " @click="reloadStencilMaster()" variant="secondary">
          📄ステンシルマスタをリロード
        </b-button>
      </div>
      <hr>
      <div>
        <form ref="form1" @submit.stop.prevent="mainHandleSubmit">
          <b-form-group
            invalid-feedback="Required item error."
          >
            <b-container fluid>
              <legend>ステンシル情報</legend>
              <b-row class="my-1">
                <b-col sm="3">
                  <label for="head_stencil_kind" class="pm_label">分類</label>
                </b-col>
                <b-col sm="9">
                  <b-form-select
                    id="head_stencil_kind"
                    v-model="fltStrStencilCategory.selected"
                    :options="fltStrStencilCategory.items"
                    :disabled="disabled || processing"
                    @change="stencilCategorySelected()"
                    required
                  />
                </b-col>
              </b-row>
              <b-row class="my-1">
                <b-col sm="3">
                  <label for="head_stencil_cd" class="pm_label">ステンシル</label>
                </b-col>
                <b-col sm="9">
                  <b-form-select
                    id="head_stencil_cd"
                    v-model="fltStrStencilCd.selected"
                    :options="fltStrStencilCd.items"
                    :disabled="disabled || processing || cateogryNoSelected"
                    @change="stencilSelected()"
                    required
                  />
                </b-col>
              </b-row>
              <b-row class="my-1">
                <b-col sm="3">
                  <label v-if="stencilConfig.description !== null" for="head_stencil_cd" class="pm_label"> ステンシルについて</label>
                </b-col>
                <b-col sm="9" style="text-align:left">
                  <span v-if="stencilConfig.description !== null">
                    {{ stencilConfig.description }}
                  </span>
                </b-col>
              </b-row>
              <b-row class="my-1">
                <b-col sm="7" />
                <b-col sm="1">
                  <label for="head_serial_no" class="pm_label">シリアル</label>
                </b-col>
                <b-col sm="4">
                  <b-form-select
                    id="head_serial_no"
                    v-model="fltStrSerialNo.selected"
                    :options="fltStrSerialNo.items"
                    :disabled="disabled || processing || stencilNoSelected"
                    @change="serialSelected()"
                    required
                  />
                </b-col>
              </b-row>
              <b-row class="my-1">
                <b-col sm="3" />
                <b-col sm="9" style="text-align:right">
                  <span v-if="stencilConfig.lastUpdateUser !== null">
                    Stencil Updated by {{ stencilConfig.lastUpdateUser }}
                  </span>
                  <br>
                </b-col>
              </b-row>
              <hr>
              <legend>データエレメント</legend>
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
            <b-button :disabled="disabled || processing || serialNoNoSelected" @click="generate()" variant="primary">
              Generate
            </b-button>
            <hr>
          </b-form-group>
        </form>
      </div>
    </div>
    <div>
      <b-modal
        id="modal-psv-dialog"
        ref="modal"
        @show="psvResetModal"
        @hidden="psvResetModal"
        @ok="psvHandleOk"
        title="実行条件（JSON形式）"
        ok-title="Apply"
        cancel-title="Cancel"
        centered
        scrollable
        size="lg"
        no-close-on-backdrop
      >
        <form ref="form" @submit.stop.prevent="psvHandleSubmit">
          <b-form-group
            :state="psvState"
            label="JSON形式で実行条件を編集できます。"
            label-for="name-input"
            invalid-feedback="Json is required"
          >
            <b-form-textarea
              id="name-input"
              :state="psvState"
              v-model="psvBody"
              rows="15"
              required
              placeholder="input json"
            />
          </b-form-group>
        </form>
      </b-modal>
    </div>
    <div>
      <bvUpload @fixFileId="fixFileId" />
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import BvUpload from '~/components/dialog/BvDownloadDialog.vue'

export default {
  layout: 'Main',
  components: { BvUpload },
  data () {
    return {
      disabled: false,
      processing: false,
      serialNoNoSelected: true,
      stencilNoSelected: true,
      cateogryNoSelected: true,
      eparams: [],
      fileNames: {},
      stencilConfig: null,
      fltStrStencilCategory: {
        'selected': '',
        'items': []
      },
      fltStrStencilCd: {
        'selected': '',
        'items': []
      },
      fltStrSerialNo: {
        'selected': '',
        'items': []
      },
      psvBody: '',
      psvState: null
    }
  },
  created () {
    // refresh
    this.clearAll()
  },
  methods: {
    async refresh () {
      this.processing = true
      this.clearParams()
      const ret = await axios.post(
        // `/api/mste/suggest`,
        '/mapi/apps/mste/api/suggest',
        { content: this.createRequest(this) }
      ).then((resp) => {
        if (!resp.data.errs === false &&
          resp.data.errs.length > 0) {
          this.bvMsgBoxErr(resp.data.errs)
          this.processing = false
          return false
        }

        if (!resp.data.model.params === false) {
          Object.assign(this.eparams, resp.data.model.params.childs)
        }
        if (!resp.data.model.stencil === false &&
          !resp.data.model.stencil.config === false) {
          this.stencilConfig = resp.data.model.stencil.config
        }

        this.fltStrStencilCategory = resp.data.model.fltStrStencilCategory
        this.fltStrStencilCd = resp.data.model.fltStrStencilCd
        this.fltStrSerialNo = resp.data.model.fltStrSerialNo

        this.processing = false
        return true
      }).catch((errors) => {
        this.bvMsgBoxErr(errors)
        this.processing = false
        return false
      })

      return ret
    },

    async reloadStencilMaster () {
      this.processing = true
      await axios.post(
        '/mapi/apps/mste/api/reloadStencilMaster',
        { content: this.createRequest(this) }
      ).then((resp) => {
        // nop
      }).catch((errors) => {
        this.bvMsgBoxErr(errors)
        this.processing = false
        return false
      })
      this.clearParams()
      this.refresh()
      this.processing = false
    },

    clearDelems () {
      this.clearParams()
      this.refresh()
    },

    async clearAll () {
      this.fltStrStencilCategory.selected = '*'
      this.fltStrStencilCd.selected = '*'
      this.fltStrSerialNo.selected = '*'
      this.cateogryNoSelected = true
      this.stencilNoSelected = true
      this.serialNoNoSelected = true
      this.clearParams()
      await this.refresh()
    },

    clearParams () {
      this.eparams = []
      this.stencilConfig = this.defaultStencilConfig()
    },

    callHistory () {
    },

    fileUpload (uploadingItemId, fileId) {
      const files = []
      if (fileId.length > 0) {
        const fileIdSplited = fileId.split(',')
        for (const i in fileIdSplited) {
          let name = 'ファイル'
          if (!this.fileNames[fileIdSplited[i]] === false) {
            name = this.fileNames[fileIdSplited[i]].fileName
          }
          files.push(
            {
              fileId: fileIdSplited[i],
              name
            }
          )
        }
      }

      this.$root.$emit('bv::show::modal', 'bv_dialog', { files, uploadMode: true, uploadingItemId })
    },
    generate () {
      this.processing = true
      axios.post(
        `/mapi/apps/mste/api/generate`,
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

    createRequest (body) {
      const pitems = {
        stencilCategoy: body.fltStrStencilCategory.selected,
        stencilCanonicalName: body.fltStrStencilCd.selected,
        serialNo: body.fltStrSerialNo.selected
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
    stencilCategorySelected () {
      this.fltStrStencilCd.selected = '*'
      this.fltStrSerialNo.selected = ''
      this.cateogryNoSelected = false
      this.stencilNoSelected = true
      this.serialNoNoSelected = true

      if (!this.isFltStrSelected(this.fltStrStencilCategory) ||
        this.fltStrStencilCategory.selected === '*') {
        this.categoryNoSelected = true
        return false
      }

      this.refresh()
      return true
    },
    stencilSelected () {
      this.fltStrSerialNo.selected = ''
      this.cateogryNoSelected = false
      this.stencilNoSelected = false
      this.serialNoNoSelected = true

      if (!this.isFltStrSelected(this.fltStrStencilCd) ||
        this.fltStrStencilCd.selected === '*') {
        this.stencilNoSelected = true
        return false
      }

      this.refresh()

      this.serialNoNoSelected = false
      // if (this.isFltStrSelected(this.fltStrSerialNo)) {
      //   this.serialNoNoSelected = false
      // }

      return true
    },
    serialSelected () {
      this.cateogryNoSelected = false
      this.stencilNoSelected = false
      this.serialNoNoSelected = false

      // if (!this.isFltStrSelected(this.fltStrSerialNo)) {
      //   this.serialNoNoSelected = true
      //   return false
      // }

      this.refresh()
      return true
    },

    isFltStrSelected (fltStr) {
      if (!fltStr) {
        return false
      }
      if (!Array.isArray(fltStr.items)) {
        return false
      }
      if (!fltStr.selected) {
        return false
      }
      if (fltStr.selected.length === 0) {
        return false
      }
      return true
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
    },

    defaultStencilConfig () {
      return {
        id: null,
        name: null,
        serial: null,
        lastUpdate: null,
        lastUpdateUser: null,
        description: null
      }
    },

    async jsonValueToParam (psvBody) {
      await this.clearAll
      const psvBodyObj = JSON.parse(psvBody)

      // category selected
      this.fltStrStencilCategory.selected = psvBodyObj.stencilCategory
      if (!await this.refresh()) {
        return
      }

      // stencil selected
      this.fltStrStencilCd.selected = psvBodyObj.stencilCd
      if (!await this.refresh()) {
        return
      }

      // serial selected
      this.fltStrSerialNo.selected = psvBodyObj.serialNo
      if (!await this.refresh()) {
        return
      }

      const eparams = Object.assign(this.eparams)
      this.eparams = []
      for (const key in psvBodyObj.dataElements) {
        const id = psvBodyObj.dataElements[key].id
        const value = psvBodyObj.dataElements[key].value
        this.setEparamById(eparams, id, value)
      }
      Object.assign(this.eparams, eparams)
    },

    setEparamById (eparams, id, value) {
      for (const key in eparams) {
        if (id === eparams[key].id) {
          eparams[key].value = value
        }
      }
    },

    paramToJsonValue (eparams) {
      if (!this.fltStrStencilCategory.selected) {
        return {}
      }
      if (!this.fltStrStencilCd.selected) {
        return {}
      }

      const dataElements = []
      for (const key in eparams) {
        const item = {
          id: eparams[key].id,
          value: eparams[key].value
        }
        dataElements.push(item)
      }
      return {
        stencilCategory: this.fltStrStencilCategory.selected,
        stencilCd: this.fltStrStencilCd.selected,
        serialNo: this.fltStrSerialNo.selected,
        dataElements
      }
    },
    psvCheckFormValidity () {
      const valid = this.$refs.form.checkValidity()
      this.psvState = valid
      return valid
    },
    psvResetModal () {
      this.psvBody = ''
      this.psvBody = JSON.stringify(this.paramToJsonValue(this.eparams), null, '  ')
      this.psvState = null
    },
    psvHandleOk (bvModalEvt) {
      bvModalEvt.preventDefault()
      this.psvHandleSubmit()
    },
    psvHandleSubmit () {
      this.processing = true
      if (!this.psvCheckFormValidity()) {
        return
      }
      this.jsonValueToParam(this.psvBody)
      this.serialSelected()
      this.$nextTick(() => {
        this.$refs.modal.hide()
      })
      this.processing = false
    },
    fixFileId (data) {
      let fileIds = ''
      for (const i in data.files) {
        this.fileNames[data.files[i].fileId] = { fileName: data.files[i].name }
        fileIds += data.files[i].fileId
        fileIds += ','
      }
      fileIds = fileIds.slice(0, -1)

      const eparams = Object.assign(this.eparams)
      this.setEparamById(eparams, data.uploadingItemId, fileIds)
      Object.assign(this.eparams, eparams)
      this.eparams.splice()

      // eslint-disable-next-line no-console
      console.log(data)
      // eslint-disable-next-line no-console
      console.log(this.fileNames)
    },
    defaultStore () {
      return {
        'selected': '',
        'items': []
      }
    }
  }
}
</script>

<style lang="css" scoped>
#fm_notes {
  white-space: pre-wrap; /* なんでダメなんだぁ。 */
}
</style>
