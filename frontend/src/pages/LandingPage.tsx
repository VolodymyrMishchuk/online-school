import { Link } from 'react-router-dom';
import { BookOpen, GraduationCap, Heart } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function LandingPage() {
    const { t } = useTranslation();
    return (
        <div className="min-h-screen bg-white font-sans text-brand-dark">
            {/* Header */}
            <header className="fixed w-full bg-white/80 backdrop-blur-md z-50 border-b border-brand-light">
                <div className="container mx-auto px-6 py-4 flex justify-between items-center">
                    <div className="flex items-center gap-2">
                        <div className="bg-brand-light p-2 rounded-full">
                            <Heart className="w-6 h-6 text-brand-secondary fill-brand-secondary" />
                        </div>
                        <span className="text-2xl font-bold text-brand-dark tracking-tight">PidSercem<span className="text-brand-primary">.School</span></span>
                    </div>
                    <nav className="flex items-center gap-8 hidden md:flex">
                        <Link to="/catalog" className="text-brand-dark hover:text-brand-primary font-medium transition-colors">{t('landing.nav.courses', 'Курси')}</Link>
                        <Link to="/about" className="text-brand-dark hover:text-brand-primary font-medium transition-colors">{t('landing.nav.about', 'Про нас')}</Link>
                        <Link to="/blog" className="text-brand-dark hover:text-brand-primary font-medium transition-colors">{t('landing.nav.blog', 'Блог')}</Link>
                    </nav>
                    <div className="flex gap-4">
                        <Link to="/catalog" className="hidden sm:block text-brand-dark hover:text-brand-primary font-medium py-2.5 px-4 transition-colors">
                            {t('landing.nav.catalog', 'Каталог')}
                        </Link>
                        <Link to="/login" className="px-6 py-2.5 rounded-full bg-brand-primary text-white font-bold hover:bg-brand-secondary transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5">
                            {t('landing.nav.login', 'Увійти')}
                        </Link>
                    </div>
                </div>
            </header>

            {/* Hero */}
            <section className="pt-32 pb-20 bg-gradient-to-b from-brand-light/30 to-white">
                <div className="container mx-auto px-6 text-center">
                    <div className="inline-block px-4 py-1.5 mb-6 rounded-full bg-brand-light text-brand-secondary font-semibold text-sm tracking-wide uppercase">
                        {t('landing.hero.badge', 'Почніть свою подорож')}
                    </div>
                    <h1 className="text-5xl md:text-7xl font-bold text-brand-dark mb-8 leading-tight">
                        {t('landing.hero.titlePart1', 'Навчання з')} <br /> <span className="text-brand-primary">{t('landing.hero.titlePart2', 'Любовʼю та Турботою')}</span>
                    </h1>
                    <p className="text-xl text-gray-600 mb-10 max-w-2xl mx-auto leading-relaxed">
                        {t('landing.hero.description', 'Підтримуюче середовище для опанування нових навичок. Курси від експертів, створені для вашого особистого та професійного розвитку.')}
                    </p>
                    <div className="flex flex-col sm:flex-row justify-center gap-4">
                        <Link to="/catalog" className="px-10 py-4 rounded-full bg-brand-secondary text-white font-bold text-lg hover:bg-brand-primary transition-all shadow-xl hover:shadow-2xl transform hover:-translate-y-1">
                            {t('landing.hero.browseBtn', 'Переглянути курси')}
                        </Link>
                        <Link to="/about" className="px-10 py-4 rounded-full bg-white text-brand-dark border-2 border-brand-light font-bold text-lg hover:border-brand-primary hover:text-brand-primary transition-all">
                            {t('landing.hero.learnMoreBtn', 'Дізнатися більше')}
                        </Link>
                    </div>
                </div>
            </section>

            {/* Decorative Wave/Separator could go here */}

            {/* Features */}
            <section className="py-24 bg-white">
                <div className="container mx-auto px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-4xl font-bold text-brand-dark mb-4">{t('landing.features.title', 'Чому обирають нас?')}</h2>
                        <p className="text-gray-500 max-w-xl mx-auto">{t('landing.features.description', 'Ми ставимо ваш досвід навчання на перше місце, використовуючи делікатний та інклюзивний підхід.')}</p>
                    </div>

                    <div className="grid md:grid-cols-3 gap-8">
                        <FeatureCard
                            icon={<BookOpen className="w-8 h-8 text-white" />}
                            title={t('landing.features.expert.title', 'Експертні знання')}
                            description={t('landing.features.expert.desc', 'Кураторський контент від лідерів індустрії, поданий у вигляді невеликих, легких для засвоєння уроків.')}
                        />
                        <FeatureCard
                            icon={<Heart className="w-8 h-8 text-white" />}
                            title={t('landing.features.community.title', 'Підтримуюча спільнота')}
                            description={t('landing.features.community.desc', 'Приєднуйтесь до мережі однодумців. Ми ростемо швидше, коли ростемо разом.')}
                        />
                        <FeatureCard
                            icon={<GraduationCap className="w-8 h-8 text-white" />}
                            title={t('landing.features.certified.title', 'Сертифікований розвиток')}
                            description={t('landing.features.certified.desc', 'Отримуйте сертифікати, які мають значення. Підтверджуйте свою наполегливу працю та нові навички.')}
                        />
                    </div>
                </div>
            </section>

            {/* Footer */}
            <footer className="bg-brand-light/20 py-12 border-t border-brand-light">
                <div className="container mx-auto px-6 text-center text-gray-500">
                    <p>&copy; 2026 PidSercem School. All rights reserved.</p>
                </div>
            </footer>
        </div>
    );
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode, title: String, description: String }) {
    return (
        <div className="group p-8 rounded-3xl bg-white border border-brand-light/50 shadow-sm hover:shadow-xl hover:border-brand-primary/30 transition-all duration-300">
            <div className="mb-6 bg-brand-primary w-16 h-16 rounded-2xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300">
                {icon}
            </div>
            <h3 className="text-2xl font-bold text-brand-dark mb-3 group-hover:text-brand-secondary transition-colors">{title}</h3>
            <p className="text-gray-600 leading-relaxed">{description}</p>
        </div>
    );
}
