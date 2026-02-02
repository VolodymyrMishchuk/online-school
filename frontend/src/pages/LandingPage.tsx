import { Link } from 'react-router-dom';
import { BookOpen, GraduationCap, Heart } from 'lucide-react';

export default function LandingPage() {
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
                        <Link to="/catalog" className="text-brand-dark hover:text-brand-primary font-medium transition-colors">Courses</Link>
                        <Link to="/about" className="text-brand-dark hover:text-brand-primary font-medium transition-colors">About</Link>
                        <Link to="/blog" className="text-brand-dark hover:text-brand-primary font-medium transition-colors">Blog</Link>
                    </nav>
                    <div className="flex gap-4">
                        <Link to="/catalog" className="hidden sm:block text-brand-dark hover:text-brand-primary font-medium py-2.5 px-4 transition-colors">
                            Catalog
                        </Link>
                        <Link to="/login" className="px-6 py-2.5 rounded-full bg-brand-primary text-white font-bold hover:bg-brand-secondary transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5">
                            Log In
                        </Link>
                    </div>
                </div>
            </header>

            {/* Hero */}
            <section className="pt-32 pb-20 bg-gradient-to-b from-brand-light/30 to-white">
                <div className="container mx-auto px-6 text-center">
                    <div className="inline-block px-4 py-1.5 mb-6 rounded-full bg-brand-light text-brand-secondary font-semibold text-sm tracking-wide uppercase">
                        Start Your Journey
                    </div>
                    <h1 className="text-5xl md:text-7xl font-bold text-brand-dark mb-8 leading-tight">
                        Learning with <br /> <span className="text-brand-primary">Love & Care</span>
                    </h1>
                    <p className="text-xl text-gray-600 mb-10 max-w-2xl mx-auto leading-relaxed">
                        A supportive space to master new skills. Expert-led courses designed for your personal and professional growth.
                    </p>
                    <div className="flex flex-col sm:flex-row justify-center gap-4">
                        <Link to="/catalog" className="px-10 py-4 rounded-full bg-brand-secondary text-white font-bold text-lg hover:bg-brand-primary transition-all shadow-xl hover:shadow-2xl transform hover:-translate-y-1">
                            Browse Courses
                        </Link>
                        <Link to="/about" className="px-10 py-4 rounded-full bg-white text-brand-dark border-2 border-brand-light font-bold text-lg hover:border-brand-primary hover:text-brand-primary transition-all">
                            Learn More
                        </Link>
                    </div>
                </div>
            </section>

            {/* Decorative Wave/Separator could go here */}

            {/* Features */}
            <section className="py-24 bg-white">
                <div className="container mx-auto px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-4xl font-bold text-brand-dark mb-4">Why Choose Us?</h2>
                        <p className="text-gray-500 max-w-xl mx-auto">We prioritize your learning experience with a gentle, inclusive approach.</p>
                    </div>

                    <div className="grid md:grid-cols-3 gap-8">
                        <FeatureCard
                            icon={<BookOpen className="w-8 h-8 text-white" />}
                            title="Expert Knowledge"
                            description="Curated content from industry leaders, delivered in bite-sized, easy-to-digest lessons."
                        />
                        <FeatureCard
                            icon={<Heart className="w-8 h-8 text-white" />}
                            title="Supportive Community"
                            description="Join a network of like-minded individuals. We grow faster when we grow together."
                        />
                        <FeatureCard
                            icon={<GraduationCap className="w-8 h-8 text-white" />}
                            title="Certified Growth"
                            description="Earn certificates that matter.validate your hard work and new skills."
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
