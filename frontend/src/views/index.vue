<template>
  <div class="fan-home">
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow"><span class="dot" /> FAN WORKSPACE</span>
        <h1>让每一次带货合作<br><em>都有清晰的进展。</em></h1>
        <p>从商家入驻、商品资料到视频发布和带货数据，在一个工作台中跟进合作。</p>
        <div class="hero-actions">
          <router-link v-if="!isAdmin" class="primary-link" to="/merchant">进入商家工作台 <span>→</span></router-link>
          <router-link v-else class="primary-link" to="/operations">进入运营工作台 <span>→</span></router-link>
        </div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="orbit orbit-one"></div><div class="orbit orbit-two"></div>
        <div class="visual-card"><span class="visual-icon">↗</span><strong>从商品到增长</strong><small>让合作信息有迹可循</small><div class="visual-bars"><i></i><i></i><i></i><i></i><i></i></div></div>
        <div class="visual-chip">✦ &nbsp; 跨境带货协作</div>
      </div>
    </section>
    <section class="section-heading"><div><span class="eyebrow dark">WORKFLOW</span><h2>合作流程</h2><p>围绕一笔订单，连接商家、商品和带货内容。</p></div></section>
    <section class="workflow">
      <article class="step"><span class="number">01</span><div class="step-icon mint">企</div><h3>商家与商品</h3><p>提交资质与商品素材，审核通过后开始合作。</p><router-link class="tag ready" :to="isAdmin ? '/operations?tab=merchants' : '/merchant'">资质与资料 →</router-link></article>
      <article class="step"><span class="number">02</span><div class="step-icon blue">单</div><h3>合作订单</h3><p>记录发布数量、服务费用及各商品佣金约定。</p><router-link class="tag ready" :to="isAdmin ? '/operations?tab=orders' : '/merchant'">合作与确认 →</router-link></article>
      <article class="step"><span class="number">03</span><div class="step-icon violet">视</div><h3>视频与效果</h3><p>登记视频及发布记录，汇总每条视频的带货 GMV。</p><router-link class="tag neutral" :to="isAdmin ? '/operations?tab=imports' : '/merchant'">发布与 GMV →</router-link></article>
      <article class="step"><span class="number">04</span><div class="step-icon amber">账</div><h3>费用与结算</h3><p>跟进服务费、佣金和结算状态，保留确认记录。</p><router-link class="tag neutral" :to="isAdmin ? '/operations?tab=settlements' : '/merchant'">费用与核对 →</router-link></article>
    </section>

  </div>
</template>
<script setup>
import { computed } from 'vue'
import useUserStore from '@/store/modules/user'
const user = useUserStore()
const isAdmin = computed(() => user.roles.includes('admin'))
</script>
<style scoped>
.fan-home{max-width:1500px;margin:0 auto;padding:26px 30px 44px;color:#182b3d;font-family:Inter,"Microsoft YaHei",sans-serif}.hero{min-height:280px;border-radius:20px;background:linear-gradient(115deg,#0d2834 0%,#133d43 62%,#19544f 100%);color:#fff;display:flex;overflow:hidden;position:relative;box-shadow:0 16px 35px rgba(21,54,58,.12)}.hero-copy{padding:44px 52px;z-index:1;flex:1}.eyebrow{font-size:11px;letter-spacing:.16em;font-weight:700;color:#9be5d2;display:inline-flex;align-items:center;gap:8px}.dot{width:7px;height:7px;border-radius:50%;background:#78dfbd}.hero h1{font-size:32px;line-height:1.35;letter-spacing:.01em;margin:17px 0 12px;font-weight:700}.hero h1 em{font-style:normal;color:#a3ecd5}.hero p{color:#c4d9d4;font-size:14px;line-height:1.8;margin:0;max-width:580px}.hero-actions{margin-top:22px}.primary-link{display:inline-flex;align-items:center;gap:24px;background:#b2ebd6;color:#123c37;border-radius:9px;padding:11px 17px;font-size:13px;font-weight:700;text-decoration:none}.admin-hint{font-size:12px;color:#d3e9df;border:1px solid #69988b;padding:8px 12px;border-radius:8px}.hero-visual{width:330px;position:relative;flex-shrink:0}.orbit{position:absolute;border:1px solid rgba(161,229,208,.18);border-radius:50%;width:300px;height:300px;right:-15px;top:-5px}.orbit-two{width:235px;height:235px;top:28px;right:18px}.visual-card{position:absolute;right:68px;top:57px;width:195px;height:164px;background:rgba(240,255,249,.12);border:1px solid rgba(226,255,243,.22);border-radius:16px;padding:18px;backdrop-filter:blur(5px);display:flex;flex-direction:column;gap:8px}.visual-icon{width:30px;height:30px;border-radius:9px;background:#b2ebd6;color:#195145;display:grid;place-items:center;font-size:19px}.visual-card strong{font-size:15px}.visual-card small{font-size:11px;color:#c3ded5}.visual-bars{height:27px;display:flex;align-items:flex-end;gap:7px;margin-top:4px}.visual-bars i{width:19px;background:#81d4b6;border-radius:3px 3px 0 0;height:35%}.visual-bars i:nth-child(2){height:55%}.visual-bars i:nth-child(3){height:42%}.visual-bars i:nth-child(4){height:76%}.visual-bars i:nth-child(5){height:100%}.visual-chip{position:absolute;right:24px;bottom:35px;background:#f5fff9;color:#276b59;border-radius:10px;padding:10px 14px;font-size:11px;font-weight:700;box-shadow:0 8px 20px #10342d55}.section-heading{display:flex;justify-content:space-between;align-items:end;margin:36px 0 18px}.eyebrow.dark{color:#36987e}.section-heading h2{font-size:22px;margin:8px 0}.section-heading p{font-size:13px;color:#738395;margin:0}.environment{color:#8292a1;font-size:12px}.workflow{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:15px}.step{background:#fff;border:1px solid #e8eef0;border-radius:14px;padding:20px;min-height:202px;position:relative;box-shadow:0 5px 18px rgba(25,53,66,.035)}.number{position:absolute;right:19px;top:20px;color:#d5e1e3;font-size:13px;font-weight:700}.step-icon{width:39px;height:39px;border-radius:11px;display:grid;place-items:center;font-size:15px;font-weight:700}.mint{background:#e1f6ed;color:#2d9b78}.blue{background:#e8f1fc;color:#5280c7}.violet{background:#f0edfc;color:#8069bc}.amber{background:#fdf2df;color:#ba8b3b}.step h3{font-size:15px;margin:18px 0 9px}.step p{font-size:12px;color:#718393;line-height:1.75;margin:0 0 15px;min-height:42px}.tag{text-decoration:none;display:inline-block;font-size:11px;border-radius:6px;padding:5px 8px}.ready{background:#eaf7f0;color:#488c70}.neutral{background:#f2f5f7;color:#80909c}.note{margin-top:20px;border:1px solid #e4efec;background:#f6faf8;border-radius:13px;display:flex;gap:13px;padding:16px 20px}.note-mark{width:23px;height:23px;border-radius:50%;display:grid;place-items:center;background:#d8f1e5;color:#408d6d;font-weight:700;flex:none}.note strong{font-size:13px}.note p{font-size:12px;color:#74877f;line-height:1.7;margin:5px 0 0}@media(max-width:1000px){.workflow{grid-template-columns:repeat(2,minmax(0,1fr))}.hero-visual{width:250px}.visual-card{right:35px}.hero-copy{padding:38px}}@media(max-width:650px){.fan-home{padding:16px}.hero-copy{padding:28px}.hero h1{font-size:25px}.hero-visual{display:none}.workflow{grid-template-columns:1fr}.section-heading{align-items:start;gap:12px;flex-direction:column}.environment{font-size:11px}}
</style>
