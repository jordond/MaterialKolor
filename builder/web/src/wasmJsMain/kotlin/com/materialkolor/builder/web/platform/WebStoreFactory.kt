package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.StoreFactory

// stub
// B-302 moves this onto localStorage through kstore-storage and reads other tabs' writes from the
// storage event. Until then records live in memory, as the text their codec writes.
internal class WebStoreFactory : StoreFactory by InMemoryStoreFactory()
