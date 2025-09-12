<template>
  <div>
    <b-modal
      id="bv_dialog"
      ref="bv_dialog"
      size="lg"
      scrollable
      centered
      ok-only
      title="File management"
      @ok="handleOk"
    >
      <div v-if="uploadMode">
        File ID : {{ fileId }}
        <b-form-file id="uploading-file" :multiple=false v-model="uploadingFile">
        </b-form-file>
        <div class="d-block">
          <b-button class="mt-2" variant="outline-info" @click="upload()">
            Upload
          </b-button>
        </div>
      </div>
      <p class="my-4">
        <ul>
          <template v-for="file in files">
            <li :key="file.fileId">
              <a href="javascript:void(0)" @click="download(file)">
                {{ file.name }}
              </a>
              <a href="javascript:void(0)" @click="delow(file.fileId)" v-if="uploadMode">削除</a>
            </li>
          </template>
        </ul>
      </p>
      <div class="d-block">
        <b-button class="mt-2" variant="outline-info" @click="downloadAll()">
          Download All
        </b-button>
      </div>
    </b-modal>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'BvDownloadDialog',
  data () {
    return {
      disabled: false,
      processing: false,
      files: [],
      uploadMode: false,
      uploadingItemId: null,
      fileId: null,
      uploadingFile: null
    }
  },
  mounted () {
    this.$root.$on('bv::show::modal', (bvEvent, param) => {
      this.files = []
      this.uploadMode = false
      if (!param.uploadMode === false) {
        this.uploadMode = param.uploadMode
      }
      if (!param.uploadingItemId === false) {
        this.uploadingItemId = param.uploadingItemId
      }
      if (!param.fileId === false) {
        this.fileId = param.fileId
      }
      /* eslint-disable no-console */
      console.log('File download dialog created.', bvEvent, param.files)
      /* eslint-enable no-console */
      for (const key in param.files) {
        this.files.push({
          name: param.files[key].name,
          fileId: param.files[key].fileId
        })
      }
    })
  },
  methods: {
    download (file) {
      /* eslint-disable no-console */
      console.log('File download will start...', file)
      /* eslint-enable no-console */
      const files = []
      files.push(file)
      this.callDownloadApi(files)
    },
    upload () {
      // cuurent version, not compatible to multi file.
      this.callUploadApi(this.uploadingFile)
    },
    downloadAll () {
      this.callDownloadApi(this.files)
    },
    initialize () {
    },
    callUploadApi (uploadingFile) {
      const formData = new FormData()
      formData.append('file', uploadingFile)
      axios.post(
        `/mapi/commons/upload`,
        formData).then((resp) => {
        if (!resp.data.errs === false &&
          resp.data.errs.length > 0) {
          this.bvMsgBoxErr(resp.data.errs)
          this.processing = false
        }
        this.uploadingFile = null
        this.files.push({
          fileId: resp.data.model.uuid,
          name: resp.data.model.fileName
        })
        this.fileId = resp.data.model.uuid
      })
    },
    callDownloadApi (files) {
      axios.post(
        `/mapi/commons/download`, {
          content: files
        }, {
          'responseType': 'blob'
        }).then((resp) => {
        if (!resp.data.errs === false &&
          resp.data.errs.length > 0) {
          this.bvMsgBoxErr(resp.data.errs)
          this.processing = false
          return
        }

        const name = resp.headers['content-disposition'].split('=')[1]
        const type = resp.headers['content-type']
        const blob = new Blob([resp.data], { type })
        const url = (window.URL || window.webkitURL).createObjectURL(blob)

        const link = document.createElement('a')
        link.href = url
        link.download = decodeURI(name)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
      }).catch((errors) => {
        /* eslint-disable no-console */
        console.log('Cougth error.', errors)
        /* eslint-enable no-console */
        this.bvMsgBoxErr(errors)
        this.processing = false
      })
    },

    delow (fileId) {
      for (const i in this.files) {
        if (fileId === this.files[i].fileId) {
          this.files.splice(i, 1)
        }
      }
      this.files.splice() // 描画のrefresh対策
    },

    bvMsgBoxErr (message) {
      if (!message || message === undefined) {
        message = 'エラーが発生しました。管理者に問い合わせてください。'
      }
      this.$bvModal.msgBoxOk(message, {
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

    handleOk (bvModalEvt) {
      bvModalEvt.preventDefault()
      this.handleSubmit()
    },

    handleSubmit () {
      const callback = {
        uploadingItemId: this.uploadingItemId,
        files: this.files
      }
      this.$emit('fixFileId', callback)
      this.$nextTick(() => {
        this.$bvModal.hide('bv_dialog')
      })
    }
  }
}
</script>
