import { useEffect } from 'react';
import HeroSection from '../components/landing/HeroSection';
import ForWhomSection from '../components/landing/ForWhomSection';
import HowItWorksSection from '../components/landing/HowItWorksSection';
import TopicsSection from '../components/landing/TopicsSection';
import ProgramSection from '../components/landing/ProgramSection';
import AuthorSection from '../components/landing/AuthorSection';
import CoursesSection from '../components/landing/CoursesSection';
import ReviewsSection from '../components/landing/ReviewsSection';
import ContactFormSection from '../components/landing/ContactFormSection';

export default function LandingPage() {
    useEffect(() => {
        // Handle hash navigation on initial load or from external links
        const handleHashChange = () => {
            const hash = window.location.hash;
            if (hash) {
                const element = document.querySelector(hash);
                if (element) {
                    setTimeout(() => {
                        element.scrollIntoView({ behavior: 'smooth' });
                    }, 100);
                }
            }
        };
        handleHashChange();
        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    return (
        <div className="w-full">
            <main>
                <HeroSection />
                <ForWhomSection />
                <HowItWorksSection />
                <TopicsSection />
                <ProgramSection />
                <AuthorSection />
                <CoursesSection />
                <ReviewsSection />
                <ContactFormSection />
            </main>
        </div>
    );
}
