import { Outlet } from 'react-router-dom';
import Navbar from '../components/landing/Navbar';
import Footer from '../components/landing/Footer';

export default function PublicLayout() {
    return (
        <div className="min-h-screen bg-[#FFF9F8] flex flex-col font-sans text-stone-800 selection:bg-brand-primary selection:text-white overflow-x-hidden">
            <Navbar />
            <main className="flex-grow flex flex-col pt-[88px]">
                <Outlet />
            </main>
            <Footer />
        </div>
    );
}
