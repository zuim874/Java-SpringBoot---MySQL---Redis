<template>
  <div class="home-page">
    <!-- ===== 毛玻璃导航 ===== -->
    <nav class="nav" :class="{ scrolled: scrolled }">
      <div class="nav-logo">XuWenYeTech</div>
      <ul class="nav-links" :class="{ open: menuOpen }">
        <li><a href="#intro" @click="closeMenu">关于</a></li>
        <li><a href="#services" @click="closeMenu">业务</a></li>
        <li><a href="#showcase" @click="closeMenu">案例</a></li>
        <li><a href="#contact" @click="closeMenu">联系</a></li>
        <li class="nav-user">
          <span class="nav-nickname">{{ nickname }}</span>
          <button class="nav-logout" @click="handleLogout">退出</button>
        </li>
      </ul>
      <button class="nav-toggle" @click="menuOpen = !menuOpen" aria-label="菜单">
        <span></span><span></span><span></span>
      </button>
    </nav>

    <!-- ===== 轮播大图 ===== -->
    <section class="hero" id="hero">
      <div v-for="(slide, idx) in slides" :key="idx"
           class="hero-slide" :class="{ active: currentSlide === idx }">
        <img :src="slide.image" :alt="slide.alt" loading="lazy">
      </div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-badge">✦ 2026 年度创新企业</div>
        <h1 class="hero-title">以科技之力<br>重塑数字未来</h1>
        <p class="hero-sub">我们融合前沿人工智能、云计算与数据智能，为企业打造面向未来的数字化解决方案。</p>
        <div class="hero-actions">
          <button class="btn-primary" @click="scrollTo('#services')">探索更多</button>
          <button class="btn-outline" @click="scrollTo('#showcase')">观看案例</button>
        </div>
      </div>
      <div class="carousel-dots">
        <button v-for="(_, idx) in slides" :key="idx"
                class="carousel-dot" :class="{ active: currentSlide === idx }"
                @click="goToSlide(idx)" :aria-label="'Slide ' + (idx+1)"></button>
      </div>
      <div class="scroll-indicator">
        <span>滚动</span>
        <div class="line"></div>
      </div>
    </section>

    <!-- ===== 关于我们 ===== -->
    <section class="section intro" id="intro">
      <div class="container">
        <div class="section-header" ref="introHeader">
          <span class="section-tag">关于我们</span>
          <h2 class="section-title">以创新驱动<span class="highlight">价值增长</span></h2>
          <p class="section-desc">我们是一支由工程师、设计师和战略家组成的团队，致力于将复杂技术转化为可落地的商业解决方案。</p>
        </div>
        <div class="intro-grid">
          <div class="intro-image" ref="introImage">
            <img src="https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=85" alt="Team collaboration" loading="lazy">
          </div>
          <div class="intro-text" ref="introText">
            <h3>十年深耕<br>赋能千家企业数字化转型</h3>
            <p>从初创公司到世界500强，我们帮助客户在人工智能、云计算、大数据分析等领域实现突破性进展。</p>
            <div class="intro-stats">
              <div><div class="stat-num">500+</div><div class="stat-label">服务企业</div></div>
              <div><div class="stat-num">98%</div><div class="stat-label">客户满意度</div></div>
              <div><div class="stat-num">12 年</div><div class="stat-label">行业经验</div></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 核心业务 ===== -->
    <section class="section services" id="services">
      <div class="container">
        <div class="section-header" ref="servicesHeader">
          <span class="section-tag">核心业务</span>
          <h2 class="section-title">全栈数字<span class="highlight">解决方案</span></h2>
          <p class="section-desc">覆盖从战略咨询到技术实施的全链路服务，助力企业在数字时代保持领先。</p>
        </div>
        <div class="cards-grid">
          <div v-for="(card, idx) in services" :key="idx"
               class="service-card" :ref="el => { if(el) cardRefs[idx] = el }">
            <div class="card-icon" v-html="card.icon"></div>
            <h3 class="card-title">{{ card.title }}</h3>
            <p class="card-desc">{{ card.desc }}</p>
            <span class="card-link">了解详情 →</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 精选案例 ===== -->
    <section class="section showcase" id="showcase">
      <div class="container">
        <div class="section-header" ref="showcaseHeader">
          <span class="section-tag">精选案例</span>
          <h2 class="section-title">用作品<span class="highlight">说话</span></h2>
          <p class="section-desc">每一个项目都是我们与客户共同打磨的精品，以下展示部分代表性案例。</p>
        </div>
        <div class="showcase-grid">
          <div v-for="(item, idx) in showcases" :key="idx"
               class="showcase-item" :ref="el => { if(el) showcaseRefs[idx] = el }">
            <img :src="item.image" :alt="item.title" loading="lazy">
            <div class="showcase-overlay">
              <h4>{{ item.title }}</h4>
              <p>{{ item.subtitle }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 联系 CTA ===== -->
    <section class="section cta-section" id="contact">
      <div class="container">
        <div class="cta-content" ref="ctaContent">
          <span class="section-tag">联系我们</span>
          <h2 class="section-title">准备好开启<br>下一个项目了吗？</h2>
          <p class="section-desc">无论您是在构思新产品，还是希望优化现有系统，我们都愿意倾听并为您提供专业建议。</p>
          <button class="cta-btn">预约免费咨询</button>
        </div>
      </div>
    </section>

    <!-- ===== 底部版权栏 ===== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-logo">XuWenYeTech</div>
        <div class="footer-links">
          <a href="#intro">关于</a>
          <a href="#services">业务</a>
          <a href="#showcase">案例</a>
          <a href="#contact">联系</a>
        </div>
        <div class="footer-copy">© 2026 XuWenYeTech. All rights reserved.</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const nickname = ref(localStorage.getItem('nickname') || '用户')
const scrolled = ref(false)
const menuOpen = ref(false)

// ===== 退出登录 =====
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}

function closeMenu() { menuOpen.value = false }

// ===== 轮播数据 =====
const slides = [
  { image: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1920&q=85', alt: 'Digital Innovation' },
  { image: 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=1920&q=85', alt: 'Technology' },
  { image: 'https://images.unsplash.com/photo-1535378917042-10a22c95931a?w=1920&q=85', alt: 'Innovation' }
]
const currentSlide = ref(0)
let carouselTimer = null

function goToSlide(idx) { currentSlide.value = idx; resetCarousel() }
function nextSlide() { currentSlide.value = (currentSlide.value + 1) % slides.length }
function resetCarousel() { clearInterval(carouselTimer); carouselTimer = setInterval(nextSlide, 5000) }

// ===== 业务卡片数据 =====
const services = [
  { icon: '🧠', title: '人工智能', desc: '基于大模型与机器学习，构建智能决策系统，实现业务流程自动化与智能化升级。' },
  { icon: '☁️', title: '云计算', desc: '提供多云架构设计、容器化部署与Serverless解决方案，弹性支撑业务高速增长。' },
  { icon: '📊', title: '数据智能', desc: '构建企业级数据中台，打通数据孤岛，以可视化洞察驱动精准商业决策。' },
  { icon: '🎨', title: '体验设计', desc: '以用户为中心的产品设计与交互创新，打造令人难忘的品牌数字体验。' }
]

// ===== 案例数据 =====
const showcases = [
  { image: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600&q=80', title: '智能数据平台', subtitle: '为某金融集团构建实时数据分析引擎' },
  { image: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=600&q=80', title: '全渠道数字化', subtitle: '助力零售品牌完成线上线下融合转型' },
  { image: 'https://images.unsplash.com/photo-1555421689-491a97ff2040?w=600&q=80', title: 'AI 客服系统', subtitle: '基于大语言模型的智能对话解决方案' }
]

// ===== 滚动渐入动画 (IntersectionObserver) =====
const introHeader = ref(null)
const introImage = ref(null)
const introText = ref(null)
const servicesHeader = ref(null)
const showcaseHeader = ref(null)
const ctaContent = ref(null)
const cardRefs = reactive({})
const showcaseRefs = reactive({})
let observer = null

onMounted(() => {
  // 导航滚动效果
  window.addEventListener('scroll', onScroll)
  // 轮播启动
  carouselTimer = setInterval(nextSlide, 5000)
  // 滚动渐入
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.10, rootMargin: '0px 0px -60px 0px' })

  const targets = [
    introHeader.value, introImage.value, introText.value,
    servicesHeader.value, showcaseHeader.value, ctaContent.value,
    ...Object.values(cardRefs), ...Object.values(showcaseRefs)
  ].filter(Boolean)
  targets.forEach(el => observer.observe(el))
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  clearInterval(carouselTimer)
  if (observer) observer.disconnect()
})

function onScroll() {
  scrolled.value = window.scrollY > 60
}

function scrollTo(selector) {
  const el = document.querySelector(selector)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
/* ===== Reset & Base ===== */
.home-page {
  font-family: 'DM Sans', -apple-system, sans-serif;
  color: #212529;
  background: #ffffff;
  line-height: 1.6;
  overflow-x: hidden;
  -webkit-font-smoothing: antialiased;
}

img { max-width: 100%; height: auto; display: block; }
a { text-decoration: none; color: inherit; }
.section { padding: 120px 0; }

.container {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 40px;
}

/* ===== Glass Navigation ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 80px;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 56px;
  background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255,255,255,0.30);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav.scrolled { background: rgba(255,255,255,0.85); box-shadow: 0 1px 40px rgba(0,0,0,0.06); }

.nav-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.6rem;
  font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0 0%, #4a9eff 50%, #7c3aed 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.03em;
}

.nav-links { display: flex; align-items: center; gap: 40px; list-style: none; margin: 0; padding: 0; }
.nav-links a {
  font-size: 0.875rem;
  font-weight: 500;
  color: #868e96;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  position: relative;
  transition: color 0.3s;
  padding: 4px 0;
}
.nav-links a::after {
  content: '';
  position: absolute;
  bottom: 0; left: 0;
  width: 0; height: 2px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  transition: width 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav-links a:hover { color: #212529; }
.nav-links a:hover::after { width: 100%; }

.nav-user { display: flex; align-items: center; gap: 16px; }
.nav-nickname {
  font-size: 0.85rem;
  font-weight: 500;
  color: #2b6cb0;
}
.nav-logout {
  padding: 6px 18px;
  border: 1px solid #dee2e6;
  border-radius: 50px;
  background: transparent;
  color: #868e96;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}
.nav-logout:hover { border-color: #dc3545; color: #dc3545; }

.nav-toggle { display: none; flex-direction: column; gap: 5px; cursor: pointer; background: none; border: none; padding: 4px; }
.nav-toggle span { display: block; width: 24px; height: 2px; background: #212529; border-radius: 2px; transition: all 0.3s; }

/* ===== Hero / Carousel ===== */
.hero {
  position: relative;
  height: 100vh;
  min-height: 700px;
  overflow: hidden;
}
.hero-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 1.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.hero-slide.active { opacity: 1; }
.hero-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(2px) saturate(1.05);
  transform: scale(1.05);
  transition: transform 8s ease;
}
.hero-slide.active img { transform: scale(1.0); }

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0,0,0,0.55) 0%, rgba(0,0,0,0.20) 40%, rgba(0,0,0,0.05) 60%, rgba(0,0,0,0.30) 100%);
}

.hero-content {
  position: absolute;
  bottom: 15%;
  left: 0;
  right: 0;
  padding: 0 56px;
  max-width: 1240px;
  margin: 0 auto;
  z-index: 2;
}
.hero-badge {
  display: inline-block;
  padding: 8px 20px;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 50px;
  color: rgba(255,255,255,0.85);
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 24px;
  animation: fadeUp 0.8s 0.3s both;
}
.hero-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: clamp(3rem, 8vw, 6.5rem);
  font-weight: 700;
  color: white;
  line-height: 1.08;
  letter-spacing: -0.03em;
  max-width: 800px;
  animation: fadeUp 0.8s 0.5s both;
}
.hero-sub {
  font-size: clamp(1rem, 2vw, 1.25rem);
  font-weight: 300;
  color: rgba(255,255,255,0.75);
  max-width: 540px;
  margin-top: 20px;
  line-height: 1.7;
  animation: fadeUp 0.8s 0.7s both;
}
.hero-actions {
  display: flex; gap: 16px; margin-top: 36px;
  animation: fadeUp 0.8s 0.9s both;
}
.btn-primary {
  padding: 16px 40px;
  border: none;
  border-radius: 50px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}
.btn-primary:hover { transform: translateY(-3px); box-shadow: 0 12px 32px rgba(43,108,176,0.40); }
.btn-outline {
  padding: 16px 40px;
  border: 1px solid rgba(255,255,255,0.30);
  border-radius: 50px;
  background: transparent;
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s;
}
.btn-outline:hover { background: rgba(255,255,255,0.10); border-color: rgba(255,255,255,0.50); }

.carousel-dots {
  position: absolute;
  bottom: 8%;
  left: 56px;
  display: flex;
  gap: 12px;
  z-index: 2;
}
.carousel-dot {
  width: 40px; height: 3px;
  background: rgba(255,255,255,0.25);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  border: none;
}
.carousel-dot.active { background: rgba(255,255,255,0.85); width: 60px; }

.scroll-indicator {
  position: absolute;
  bottom: 40px;
  right: 56px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(255,255,255,0.40);
  font-size: 0.65rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  animation: float 2s ease-in-out infinite;
}
.scroll-indicator .line {
  width: 1px; height: 40px;
  background: linear-gradient(to bottom, rgba(255,255,255,0.40), transparent);
}

/* ===== Section Header ===== */
.section-header {
  text-align: center;
  max-width: 680px;
  margin: 0 auto 72px;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.section-header.visible { opacity: 1; transform: translateY(0); }

.section-tag {
  display: inline-block;
  padding: 6px 16px;
  background: #f1f3f5;
  border-radius: 50px;
  font-size: 0.7rem;
  font-weight: 600;
  color: #868e96;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 16px;
}
.section-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: clamp(2rem, 4vw, 3.2rem);
  font-weight: 700;
  color: #212529;
  line-height: 1.2;
  letter-spacing: -0.02em;
}
.section-title .highlight {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.section-desc {
  font-size: 1.05rem;
  color: #868e96;
  margin-top: 16px;
  line-height: 1.7;
}

/* ===== Intro / About ===== */
.intro { background: #f8f9fa; }
.intro-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
}
.intro-image {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.08);
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.intro-image.visible { opacity: 1; transform: translateY(0); }
.intro-image img {
  width: 100%;
  height: 500px;
  object-fit: cover;
  filter: blur(1px) saturate(1.08);
  transition: transform 0.6s;
}
.intro-image:hover img { transform: scale(1.02); }

.intro-text {
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94) 0.15s;
}
.intro-text.visible { opacity: 1; transform: translateY(0); }
.intro-text h3 {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 2rem;
  color: #212529;
  line-height: 1.3;
  margin-bottom: 20px;
}
.intro-text p { color: #868e96; line-height: 1.8; font-size: 0.95rem; }

.intro-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  margin-top: 40px;
  padding-top: 40px;
  border-top: 1px solid #dee2e6;
}
.stat-num {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 2.4rem;
  font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.stat-label { font-size: 0.8rem; color: #868e96; margin-top: 4px; letter-spacing: 0.04em; text-transform: uppercase; }

/* ===== Services Cards ===== */
.services { background: #ffffff; }
.cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.service-card {
  background: #ffffff;
  border: 1px solid #dee2e6;
  border-radius: 24px;
  padding: 40px 32px;
  transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
  cursor: default;
  opacity: 0;
  transform: translateY(40px);
}
.service-card.visible { opacity: 1; transform: translateY(0); }
.service-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.service-card:hover { transform: translateY(-8px); box-shadow: 0 20px 60px rgba(0,0,0,0.08); border-color: transparent; }
.service-card:hover::before { transform: scaleX(1); }
.service-card.visible:hover { transform: translateY(-8px); }

.card-icon {
  width: 56px; height: 56px;
  border-radius: 16px;
  background: #f1f3f5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  font-size: 1.5rem;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.service-card:hover .card-icon {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
  box-shadow: 0 8px 24px rgba(43,108,176,0.25);
}
.card-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 12px;
  color: #212529;
}
.card-desc { font-size: 0.875rem; color: #868e96; line-height: 1.7; }
.card-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  color: #2b6cb0;
  transition: gap 0.3s;
}
.service-card:hover .card-link { gap: 12px; }

/* ===== Showcase ===== */
.showcase { background: #f8f9fa; }
.showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.showcase-item {
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  aspect-ratio: 4/3;
  cursor: pointer;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.showcase-item.visible { opacity: 1; transform: translateY(0); }
.showcase-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(1px) saturate(1.05);
  transition: all 0.7s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.showcase-item:hover img { transform: scale(1.06); filter: blur(0) saturate(1.1); }

.showcase-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.60) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 32px;
}
.showcase-item:hover .showcase-overlay { opacity: 1; }
.showcase-overlay h4 { color: white; font-family: 'Playfair Display', Georgia, serif; font-size: 1.2rem; margin-bottom: 4px; }
.showcase-overlay p { color: rgba(255,255,255,0.60); font-size: 0.8rem; }

/* ===== CTA ===== */
.cta-section {
  background: #212529;
  position: relative;
  overflow: hidden;
}
.cta-section::before {
  content: '';
  position: absolute;
  top: -50%; right: -20%;
  width: 600px; height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(74,158,255,0.08), transparent 70%);
}
.cta-section::after {
  content: '';
  position: absolute;
  bottom: -30%; left: -10%;
  width: 400px; height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124,58,237,0.06), transparent 70%);
}
.cta-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 640px;
  margin: 0 auto;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.cta-content.visible { opacity: 1; transform: translateY(0); }
.cta-content .section-tag { background: rgba(255,255,255,0.06); color: #adb5bd; border: 1px solid rgba(255,255,255,0.06); }
.cta-content .section-title { color: white; }
.cta-content .section-desc { color: #adb5bd; }

.cta-btn {
  display: inline-block;
  margin-top: 36px;
  padding: 18px 52px;
  border: none;
  border-radius: 50px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.cta-btn:hover { transform: translateY(-4px); box-shadow: 0 16px 40px rgba(43,108,176,0.35); }

/* ===== Footer ===== */
.footer {
  background: #212529;
  border-top: 1px solid rgba(255,255,255,0.06);
  padding: 48px 56px;
}
.footer-inner {
  max-width: 1240px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.footer-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.1rem;
  font-weight: 700;
  color: rgba(255,255,255,0.60);
  letter-spacing: -0.02em;
}
.footer-links { display: flex; gap: 32px; }
.footer-links a { font-size: 0.75rem; color: #868e96; letter-spacing: 0.04em; text-transform: uppercase; transition: color 0.3s; }
.footer-links a:hover { color: #ced4da; }
.footer-copy { font-size: 0.75rem; color: #495057; }

/* ===== Keyframes ===== */
@keyframes fadeUp { to { opacity: 1; transform: translateY(0); } }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(8px); } }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .cards-grid { grid-template-columns: repeat(2, 1fr); }
  .showcase-grid { grid-template-columns: repeat(2, 1fr); }
  .intro-grid { gap: 48px; }
}
@media (max-width: 768px) {
  .nav { padding: 0 24px; }
  .nav-links {
    display: none;
    flex-direction: column;
    position: absolute;
    top: 80px; left: 0; right: 0;
    background: rgba(255,255,255,0.92);
    backdrop-filter: blur(20px);
    padding: 24px 32px;
    gap: 20px;
    border-bottom: 1px solid #dee2e6;
  }
  .nav-links.open { display: flex; }
  .nav-toggle { display: flex; }
  .nav-user { margin-top: 8px; padding-top: 16px; border-top: 1px solid #dee2e6; width: 100%; justify-content: space-between; }
  .hero-content { padding: 0 24px; bottom: 20%; }
  .hero-actions { flex-direction: column; gap: 12px; }
  .section { padding: 80px 0; }
  .container { padding: 0 24px; }
  .intro-grid { grid-template-columns: 1fr; gap: 40px; }
  .intro-image img { height: 300px; }
  .cards-grid { grid-template-columns: 1fr; }
  .showcase-grid { grid-template-columns: 1fr; }
  .carousel-dots { left: 24px; }
  .scroll-indicator { right: 24px; bottom: 24px; }
  .footer { padding: 40px 24px; }
  .footer-inner { flex-direction: column; gap: 24px; text-align: center; }
  .footer-links { flex-wrap: wrap; justify-content: center; gap: 20px; }
}
@media (max-width: 480px) {
  .intro-stats { grid-template-columns: 1fr; gap: 20px; }
}
</style>