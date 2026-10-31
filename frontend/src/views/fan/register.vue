<template>
  <main class="page"><el-card class="card"><h2>商家入驻</h2><p>注册后提交资质，审核通过后可登记商品。</p>
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="商家名称"><el-input v-model="form.name" maxlength="200" /></el-form-item>
      <el-form-item label="统一社会信用代码"><el-input v-model="form.creditCode" maxlength="64" /></el-form-item>
      <el-form-item label="联系人"><el-input v-model="form.contactName" maxlength="100" /></el-form-item>
      <el-form-item label="联系电话"><el-input v-model="form.contactPhone" maxlength="40" /></el-form-item>
      <el-form-item label="联系邮箱（可选）"><el-input v-model="form.email" maxlength="200" /></el-form-item>
      <el-form-item label="登录账号"><el-input v-model="form.username" maxlength="20" /></el-form-item>
      <el-form-item label="登录密码"><el-input v-model="form.password" type="password" show-password maxlength="20" /></el-form-item>
      <el-form-item v-if="captchaEnabled" label="验证码"><div class="captcha"><el-input v-model="form.code" /><img :src="captchaUrl" alt="点击刷新验证码" @click="loadCaptcha" /></div></el-form-item>
      <el-button type="primary" :loading="busy" @click="submit">提交注册</el-button>
      <router-link to="/login" class="link">已有账号，去登录</router-link>
    </el-form>
  </el-card></main>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCodeImg } from '@/api/login'
import request from '@/utils/request'
const router=useRouter(), busy=ref(false),captchaEnabled=ref(true),captchaUrl=ref('')
const form=reactive({name:'',creditCode:'',contactName:'',contactPhone:'',email:'',username:'',password:'',code:'',uuid:''})
async function loadCaptcha(){const r=await getCodeImg();captchaEnabled.value=r.captchaEnabled!==false;captchaUrl.value='data:image/gif;base64,'+r.img;form.uuid=r.uuid||''}
async function submit(){if(!form.name.trim()||!form.creditCode.trim()||!form.contactName.trim()||!form.contactPhone.trim()||!form.username.trim()||!form.password){ElMessage.warning('请填写商家及账号信息');return}busy.value=true;try{await request({url:'/fan/merchant/register',method:'post',headers:{isToken:false},data:form});ElMessage.success('注册成功，请登录并提交资质');await router.push('/login')}catch(e){await loadCaptcha()}finally{busy.value=false}}
onMounted(loadCaptcha)
</script>
<style scoped>.page{min-height:100vh;background:#f3f6fa;display:flex;justify-content:center;padding:32px}.card{width:min(520px,100%);height:max-content}.card p{color:#667085}.captcha{display:flex;gap:12px;width:100%}.captcha img{height:40px;cursor:pointer}.link{margin-left:16px}</style>
