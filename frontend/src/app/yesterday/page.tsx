"use client"

import React from "react";

import {SearchBar} from "@/components/custom/SearchBar"
import { useSearchParams } from "next/navigation";
import {BookSearchResult} from "@/app/yesterday/components/BookSearchResult"
import { YesterdayMain } from "./components/YesterdayMain";

export default function Yesterday() {
  const searchParams = useSearchParams()

  const searchQuery = searchParams && searchParams.get("q");

  return (
    <div>
      <SearchBar defaultValue={""} placeholder="봄날의 서, 작가 검색.."></SearchBar>
      {searchQuery ? <BookSearchResult query = {searchQuery}/> : <YesterdayMain/>}
    </div>
  );
}
