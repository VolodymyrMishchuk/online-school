import { useState, useEffect, useRef } from 'react';
import { ChevronDown, Check, X } from 'lucide-react';

interface Option {
    value: string;
    label: string;
}

interface MultiSelectProps {
    label: string;
    options: Option[];
    selectedValues: string[];
    onChange: (values: string[]) => void;
    placeholder?: string;
}

export default function MultiSelect({
    label,
    options,
    selectedValues,
    onChange,
    placeholder = 'Select...'
}: MultiSelectProps) {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    const toggleOption = (value: string) => {
        const newSelected = selectedValues.includes(value)
            ? selectedValues.filter(v => v !== value)
            : [...selectedValues, value];
        onChange(newSelected);
    };

    const clearSelection = (e: React.MouseEvent) => {
        e.stopPropagation();
        onChange([]);
    };

    const selectedLabels = options
        .filter(opt => selectedValues.includes(opt.value))
        .map(opt => opt.label);

    const displayValue = selectedValues.length === 0
        ? placeholder
        : selectedValues.length === 1
            ? selectedLabels[0]
            : `${selectedValues.length} вибрано`;

    return (
        <div className="relative min-w-[200px]" ref={dropdownRef}>
            {label && (
                <label className="block text-sm font-medium text-gray-700 mb-1">
                    {label}
                </label>
            )}
            <div
                onClick={() => setIsOpen(!isOpen)}
                className={`
                    w-full px-4 py-2 rounded-lg border bg-white text-sm cursor-pointer
                    flex items-center justify-between
                    transition-all duration-200
                    ${isOpen ? 'border-brand-primary ring-2 ring-brand-primary/20' : 'border-gray-200 hover:border-brand-primary/50'}
                `}
            >
                <span className={`truncate ${selectedValues.length === 0 ? 'text-gray-500' : 'text-gray-900'}`}>
                    {displayValue}
                </span>
                <div className="flex items-center gap-2">
                    {selectedValues.length > 0 && (
                        <div
                            onClick={clearSelection}
                            className="p-1 hover:bg-gray-100 rounded-full text-gray-400 hover:text-red-500 transition-colors"
                        >
                            <X className="w-3 h-3" />
                        </div>
                    )}
                    <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
                </div>
            </div>

            {isOpen && (
                <div className="absolute z-50 w-full mt-2 bg-white border border-gray-100 rounded-lg shadow-xl max-h-60 overflow-auto animate-in fade-in zoom-in-95 duration-100">
                    <div className="p-1">
                        {options.length === 0 ? (
                            <div className="px-4 py-3 text-sm text-gray-400 text-center">
                                Немає опцій
                            </div>
                        ) : (
                            options.map((option) => {
                                const isSelected = selectedValues.includes(option.value);
                                return (
                                    <div
                                        key={option.value}
                                        onClick={() => toggleOption(option.value)}
                                        className={`
                                            flex items-center gap-3 px-3 py-2 rounded-md cursor-pointer text-sm
                                            transition-colors duration-150
                                            ${isSelected ? 'bg-brand-primary/5 text-brand-dark font-medium' : 'text-gray-600 hover:bg-gray-50'}
                                        `}
                                    >
                                        <div className={`
                                            w-4 h-4 rounded border flex items-center justify-center transition-colors
                                            ${isSelected ? 'bg-brand-primary border-brand-primary' : 'border-gray-300 bg-white'}
                                        `}>
                                            {isSelected && <Check className="w-3 h-3 text-white" />}
                                        </div>
                                        <span className="truncate">{option.label}</span>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
