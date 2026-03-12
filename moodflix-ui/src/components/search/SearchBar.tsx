"use client";

import { FC, useState, useEffect, FormEvent, ChangeEvent } from "react";
import { Search, CornerDownLeft, X } from "lucide-react";
import Typewriter from "typewriter-effect";

interface SearchBarProps {
  onQueryChange: (val: string) => void;
  onTriggerSearch: (val: string) => void;
  initialValue?: string;
}

const SearchBar: FC<SearchBarProps> = ({
  onQueryChange,
  onTriggerSearch,
  initialValue = "",
}) => {
  const [localValue, setLocalValue] = useState(initialValue);
  const [isFocused, setIsFocused] = useState(false);
  const [placeholder, setPlaceholder] = useState("");
  const placeholderExamples = [
    "a cozy rainy afternoon",
    "feeling like a villain",
    "existential dread",
    "pure brain rot",
  ];

  useEffect(() => {
    setLocalValue(initialValue);
  }, [initialValue]);

  useEffect(() => {
    let i = 0;
    const interval = setInterval(() => {
      setPlaceholder(placeholderExamples[i % placeholderExamples.length]);
      i++;
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleFocus = () => setIsFocused(true);
  const handleBlur = () => setIsFocused(false);

  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setLocalValue(newValue);
    // Report change to parent instantly so pills can update their color
    onQueryChange(newValue);
  };

  const handleFormSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (localValue.trim()) {
      onTriggerSearch(localValue);
    }
  };

  const handleClearInput = () => {
    setLocalValue("");
    onQueryChange(""); // Clears pills and results in parent
  };

  return (
    <form
      onSubmit={handleFormSubmit}
      className="relative flex w-full mx-auto group animate__animated animate__fadeIn animate__delay-1s"
    >
      {/* Search Icon (Fixed visibility) and Clear Button */}
      <div className="absolute inset-y-0 left-6 flex items-center  z-30">
        {!localValue && (
          <Search
            className="text-white/60 group-focus-within:text-brand-red pointer-events-none transition-colors"
            size={22}
          />
        )}
        {localValue && (
          <button
            type="button"
            onClick={handleClearInput}
            className="p-2 hover:bg-white/10 rounded-full text-zinc-400 hover:text-white transition-colors"
          >
            <X size={20} />
          </button>
        )}
      </div>

      {/* Input Field */}
      <div className="relative w-full">
        <input
          type="text"
          value={localValue}
          placeholder={isFocused ? "What are you in the mood for?" : ""}
          onFocus={handleFocus}
          onBlur={handleBlur}
          onChange={handleInputChange}
          className="w-full py-4 md:py-6 pl-16 pr-16 md:pr-32 text-sm sm:text-lg md:text-xl 
        bg-white/5 border border-white/20 rounded-full text-white  
        backdrop-blur-md shadow-2xl transition-all focus:outline-none focus:ring-2 focus:ring-brand-red focus:bg-white/10"
        />
        {!localValue && !isFocused && (
          <div
            className="absolute inset-0 flex items-center pl-16 text-white/40 text-base md:text-xl pointer-events-none z-0
            animate__animated animate__fadeIn"
          >
            <Typewriter
              onInit={(typewriter) => {
                typewriter
                  .pauseFor(100) // 1.5 second delay before it starts typing the first word
                  .typeString(placeholderExamples[0])
                  .pauseFor(2000)
                  .start();
              }}
              options={{
                strings: placeholderExamples,
                autoStart: true,
                loop: true,
                delay: 80,
                deleteSpeed: 30,
                cursorClassName: "text-brand-red ml-1",
              }}
            />
          </div>
        )}
      </div>

      {/* Enter Key Hint with Search */}
      <div className="absolute right-2 inset-y-0 flex items-center z-20">
        <button
          type="submit"
          className="px-3 py-1.5 rounded-lg opacity-40 hover:text-brand-red hover:opacity-100 cursor-pointer group-focus-within:opacity-100 transition-opacity"
        >
          <div className="flex items-center rounded-md p-1 gap-2 group-hover/btn:bg-black/20 transition-colors">
            <span className="hidden md:block text-sm font-bold uppercase tracking-widest pl-1">
              Search
            </span>
            <CornerDownLeft size={14} className="text-white" />
          </div>
        </button>
      </div>
    </form>
  );
};

export default SearchBar;
